package org.nustaq.kontraktor.remoting.http;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.fourk.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.serialization.FSTConfiguration;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by ruedi on 12.05.2015.
 *
 * A longpoll+shortpoll+nopoll based connector. Binary/MinBin/Json coding using POST requests is supported
 *
 * Algorithm/Expected client behaviour:
 *
 * Longpoll request is held until an event occurs or timeout. An incoming lp request reports
 * last seen sequence in its url ../sessionId/sequence such that in case of network failure
 * messages can be delivered more than once from a short (outgoing) message history.
 *
 * Regular requests are expected to come in ordered (so next request is done only if first one was replied).
 * This is necessary due to http 1.1 limitations (multiple connections+reordering would be required otherwise).
 * Responses to regular requests (actor messages) are piggy backed with long-poll-data if avaiable. This may
 * lead to out of sequence long polls (network race). So a client has to implement sequence checking in order
 * to prevent double processing of incoming messages.
 *
 * For shortpoll, a client sends "{ 'SP', sequence }" to indicate the poll request should return immediately.
 * With many clients and connection limited clients (browser,mobiles) a short poll with larger intervals (>3s) might scale better
 * at cost of latency.
 *
 * When used no-poll, streaming results to a callback is not supported. Only 'tell' (void methods) and 'ask' (IPromise-returning)
 * messages can be used.
 *
 * TODO: close session in case of unrecoverable loss
 * TODO: support temporary/discardable websocket connections as a LP optimization.
 * TODO: investigate optional use of http 2.0
 */
public class UndertowHttpServerConnector implements ActorServerConnector, HttpHandler {

    public static int REQUEST_TIMEOUT = 5000; // max wait time for a returned promise to fulfil
    public static long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30); // 30 minutes

    public static Promise<ActorServer> Publish( Actor facade, String hostName, String urlPath, int port, Coding coding ) {
        ActorServer actorServer;
        try {
            facade.setThrowExWhenBlocked(true);
            Pair<PathHandler, Undertow> serverPair = Http4K.get().getServer(port, hostName);
            UndertowHttpServerConnector con = new UndertowHttpServerConnector(facade);
            actorServer = new ActorServer( con, facade, coding == null ? new Coding(SerializerType.FSTSer) : coding );
            actorServer.start();
            serverPair.getFirst().addPrefixPath(urlPath, con);
        } catch (Exception e) {
            Log.Warn(null,e);
            return new Promise<>(null,e);
        }
        return new Promise<>(actorServer);
    }

    Actor facade;
    HashMap<String,HttpObjectSocket> sessions = new HashMap<>(); // use only from facade thread

    FSTConfiguration conf = FSTConfiguration.createJsonConfiguration(); // used for authdata
    Function<ObjectSocket, ObjectSink> factory;
    long sessionTimeout = SESSION_TIMEOUT_MS;
    volatile boolean isClosed = false;

    public UndertowHttpServerConnector(Actor facade) {
        this.facade = facade;
        facade.delayed( HttpObjectSocket.LP_TIMEOUT/2, () -> houseKeeping() );
    }

    public void houseKeeping() {
//        System.out.println("----------------- HOUSEKEEPING ------------------------");
        long now = System.currentTimeMillis();
        ArrayList<String> toRemove = new ArrayList<>(0);
        sessions.entrySet().forEach( entry -> {
            HttpObjectSocket socket = entry.getValue();
            if ( now- socket.getLongPollTaskTime() >= HttpObjectSocket.LP_TIMEOUT/2 ) {
                socket.triggerLongPoll();
            }
            if ( now- socket.getLastUse() > getSessionTimeout() ) {
                toRemove.add(entry.getKey());
            }
        });
        toRemove.forEach( sessionId -> closeSession(sessionId) );
        if ( ! isClosed ) {
            facade.delayed( HttpObjectSocket.LP_TIMEOUT/4, () -> houseKeeping() );
        }
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * if relpath is empty, this is considered as a connect/open and a session id is sent back.
     * Else /sessionId/sequ or just /sessionId is expected
     * posted content might contain an authentication information related object
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if ( exchange.getRequestMethod() != Methods.POST ) {
            exchange.setResponseCode(404);
            exchange.endExchange();
            return;
        }
        String rpath = exchange.getRelativePath();

        StreamSourceChannel requestChannel = exchange.getRequestChannel();
        String first = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        int len = Integer.parseInt(first);

        // read post data.
        ByteBuffer buf = ByteBuffer.allocate(len);
        requestChannel.getReadSetter().set( streamSourceChannel -> {
                try {
                    streamSourceChannel.read(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if ( buf.remaining() == 0 ) {
                    try {
                        requestChannel.shutdownReads();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if ( facade.isMailboxPressured() || facade.isCallbackQPressured() ) {
                        exchange.setResponseCode(503);
                        exchange.endExchange();
                    }
                    // switch to actor thread
                    facade.execute( () -> requestReceived( exchange, buf.array(), rpath ) );
                }
            }
        );
        requestChannel.resumeReads();
    }

    protected void requestReceived( HttpServerExchange exchange, byte[] postData, String path) {
        // already executed in facade thread
        while ( path.startsWith("/") )
            path = path.substring(1);

        if ( path.trim().length() > 0 ) {
            String[] split = path.split("/");
            HttpObjectSocket httpObjectSocket = sessions.get(split[0]);
            if ( httpObjectSocket != null ) {
                handleClientRequest(exchange, httpObjectSocket, postData, split.length > 1 ? split[1] : null);
            } else {
                exchange.setResponseCode(404);
                exchange.endExchange();
            }
        } else { // new session
            Object auth = null;
            // create connection. postdata is auth data
            if ( postData != null && postData.length > 0 ) {
                auth = conf.asObject(postData);
            }

            // auth check goes here

            String sessionId = Long.toHexString((long) (Math.random() * Long.MAX_VALUE));
            HttpObjectSocket sock = new HttpObjectSocket( sessionId, () -> facade.execute( () -> closeSession(sessionId))) {
                @Override
                protected int getObjectMaxBatchSize() {
                    // huge batch size to make up for stupid sync http 1.1 protocol enforcing latency inclusion
                    return HttpObjectSocket.HTTP_BATCH_SIZE*4;
                }
            };
            sessions.put( sock.getSessionId(), sock );
            ObjectSink sink = factory.apply(sock);
            sock.setSink(sink);

            // send auth response
            byte[] response = conf.asByteArray(sock.getSessionId());
            ByteBuffer responseBuf = ByteBuffer.wrap(response);

            exchange.setResponseCode(200);
            exchange.setResponseContentLength(response.length);
            StreamSinkChannel sinkchannel = exchange.getResponseChannel();
            sinkchannel.getWriteSetter().set(
                channel -> {
                    if ( responseBuf.remaining() > 0 )
                        try {
                            sinkchannel.write(responseBuf);
                            if (responseBuf.remaining() == 0) {
                                Log.Info(this, "client connected " + sessionId);
                                exchange.endExchange();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            exchange.endExchange();
                        }
                    else
                    {
                        Log.Info(this,"client connected "+sessionId);
                        exchange.endExchange();
                    }
                }
            );
            sinkchannel.resumeWrites();
        }
    }

    protected HttpObjectSocket closeSession(String sessionId) {
        Log.Info(this,sessionId+" closed");
        return sessions.remove(sessionId);
    }

    public void handleClientRequest(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, byte[] postData, String lastSeenSequence) {
        // executed in facade thread
        httpObjectSocket.updateTimeStamp(); // keep alive

        Object received = httpObjectSocket.getConf().asObject(postData);

        boolean isEmptyLP = received instanceof Object[] && ((Object[]) received).length == 1 && ((Object[]) received)[0] instanceof Number;

        // dispatch incoming messages to actor(s)

        StreamSinkChannel sinkchannel = exchange.getResponseChannel();
        if ( ! isEmptyLP ) {
            handleRegularRequest(exchange, httpObjectSocket, received, sinkchannel);
            return;
        }

        // long poll request

        // parse sequence
        int lastClientSeq = -1;
        if ( lastSeenSequence!=null ) {
            try {
                lastClientSeq = Integer.parseInt(lastSeenSequence);
            } catch (Throwable t) {
                Log.Warn(this,t);
            }
        }

        // check iif can be served from history
        if (lastClientSeq > 0 ) { // if lp response message has been sent, take it from history
            byte[] msg = (byte[]) httpObjectSocket.takeStoredLPMessage(lastClientSeq + 1);
            if (msg!=null) {
//                Log.Warn(this, "serve lp from history " + (lastClientSeq + 1) + " cur " + httpObjectSocket.getSendSequence());
                replyFromHistory(exchange, sinkchannel, msg);
                return;
            }
        }

        // new longpoll request ..

        sinkchannel.resumeWrites();
        // read next batch of pending messages from binary queue and send them
        Runnable lpTask = createLongPollTask(exchange, httpObjectSocket, sinkchannel);
        // release previous long poll request if present
        if ( httpObjectSocket.getLongPollTask() != null ) {
            httpObjectSocket.triggerLongPoll();
        }
        httpObjectSocket.setLongPollTask(lpTask);
    }

    protected Runnable createLongPollTask(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, StreamSinkChannel sinkchannel) {
        return () -> {
            Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
            byte response[] = nextQueuedMessage.getFirst();
            exchange.setResponseContentLength(response.length);
            if (response.length == 0) {
                exchange.endExchange();
            } else {
                httpObjectSocket.storeLPMessage(nextQueuedMessage.getSecond(), response);

                ByteBuffer responseBuf = ByteBuffer.wrap(response);

                if ( 1 != 0 ) {
                    while (responseBuf.remaining()>0) {
                        try {
                            sinkchannel.write(responseBuf);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    exchange.endExchange();
                } else {
                    // fixme .. this is too late as resumewrites has already been called
                    sinkchannel.getWriteSetter().set(
                        channel -> {
                            if (responseBuf.remaining() > 0)
                                try {
                                    sinkchannel.write(responseBuf);
                                    if (responseBuf.remaining() == 0) {
                                        exchange.endExchange();
                                    } else
                                        sinkchannel.resumeWrites(); // required ?
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    exchange.endExchange();
                                }
                            else {
                                exchange.endExchange();
                            }
                        }
                    );
                    try {
                        sinkchannel.write(responseBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (responseBuf.remaining() == 0) {
                        exchange.endExchange();
                    } else
                        sinkchannel.resumeWrites(); // required ?
                }
            }
        };
    }

    protected void replyFromHistory(HttpServerExchange exchange, StreamSinkChannel sinkchannel, byte[] msg) {
        ByteBuffer responseBuf = ByteBuffer.wrap(msg);
        exchange.setResponseContentLength(msg.length);
        sinkchannel.getWriteSetter().set(
            channel -> {
                if (responseBuf.remaining() > 0)
                    try {
                        sinkchannel.write(responseBuf);
                        if (responseBuf.remaining() == 0) {
                            exchange.endExchange();
                        } else
                            sinkchannel.resumeWrites(); // required ?
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.endExchange();
                    }
                else {
                    exchange.endExchange();
                }
            }
        );
        sinkchannel.resumeWrites();
    }

    protected void handleRegularRequest(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, Object received, StreamSinkChannel sinkchannel) {
        ArrayList<IPromise> futures = new ArrayList<>();
        httpObjectSocket.getSink().receiveObject(received, futures, 0);

        Runnable reply = () -> {
            // piggy back outstanding lp messages, outstanding lp request is untouched
            Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
            byte response[] = nextQueuedMessage.getFirst();
            exchange.setResponseContentLength(response.length);
            if (response.length == 0) {
                exchange.endExchange();
            } else {
                httpObjectSocket.storeLPMessage(nextQueuedMessage.getSecond(), response);

                ByteBuffer responseBuf = ByteBuffer.wrap(response);
                while (responseBuf.remaining()>0) {
                    try {
                        sinkchannel.write(responseBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                exchange.endExchange();
            }
        };
        if ( futures == null || futures.size() == 0 ) {
            reply.run();
        } else {
            Actors.all((List) futures).timeoutIn(REQUEST_TIMEOUT).then(() -> {
                reply.run();
            });
            sinkchannel.resumeWrites();
        }
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        this.facade = facade;
        this.factory = factory;
    }

    @Override
    public IPromise closeServer() {
        isClosed = true;
        return new Promise<>(null); // FIXME: should wait for real finish
    }


    public static class HTTPA extends Actor<HTTPA> {

        int count = 0;
        public IPromise hello(String s) {
            System.out.println("received "+count++);
            return resolve(count-1);
        }

        public void ui() {
            System.out.println("ui");
        }

        public void cb( Callback cb ) {
            for ( int i = 0; i < 10; i++ ) {
                if ( i < 9 ) {
                    cb.stream(i);
                }
                else
                    cb.finish();
            }

        }
    }

    public static void main( String a[] ) throws Exception {

        HTTPA facade = Actors.AsActor(HTTPA.class);
        Publish(facade,"localhost","myservice",8080,null);

        Thread.sleep(100000000);
    }
}

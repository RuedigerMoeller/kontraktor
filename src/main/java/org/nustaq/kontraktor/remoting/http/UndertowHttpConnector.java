package org.nustaq.kontraktor.remoting.http;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.UndertowWebsocketServerConnector;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.serialization.FSTConfiguration;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

/**
 * Created by ruedi on 12.05.2015.
 *
 * A longpoll based connector. Only MinBin coding using POST requests is supported
 *
 * Algorithm:
 *
 * Longpoll request is held until an event occurs or timeout.
 *
 * A method/callback call from client side is also piggy backed but not used as a longpoll request.
 * If a client does a regular remote call and there is already a long poll request in place, the long poll
 * gets replaced and the new request is kept as long poll request instead. The previous poll returns
 * empty then.
 *
 */
public class UndertowHttpConnector implements ActorServerConnector, HttpHandler {

    Actor facade;
    HashMap<String,HttpObjectSocket> sessions = new HashMap<>(); // use only from facade thread

    FSTConfiguration conf = FSTConfiguration.createMinBinConfiguration(); // used for authdata
    Function<ObjectSocket, ObjectSink> factory;

    public UndertowHttpConnector(Actor facade) {
        this.facade = facade;
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
                handlePoll(exchange, httpObjectSocket, postData, split.length > 1 ? split[1] : null );
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

            HttpObjectSocket sock = new HttpObjectSocket(Long.toHexString((long) (Math.random()*Long.MAX_VALUE)));
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
                                exchange.endExchange();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            exchange.endExchange();
                        }
                    else
                    {
                        exchange.endExchange();
                    }
                }
            );
            sinkchannel.resumeWrites();
        }
    }

    public void handlePoll(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, byte[] postData, String sequence) {
        // already executed in facade thread
//        System.out.println("req "+postData.length);
        httpObjectSocket.updateTimeStamp(); // keep alive

        Object received = httpObjectSocket.getConf().asObject(postData);

        boolean isEmptyLP = received instanceof Object[] && ((Object[]) received).length == 1 && ((Object[]) received)[0] instanceof Number;
        // dispatch incoming messages
        if ( ! isEmptyLP ) {
            httpObjectSocket.getSink().receiveObject(received);
            exchange.endExchange();
            return;
        }

        StreamSinkChannel sinkchannel = exchange.getResponseChannel();
        sinkchannel.resumeWrites();

        // read next batch of pending messages from binary queue and send them
        // fixme: could be zero copy (complicated requires reserve operation on q), at least reuse byte array per objectsocket
        Runnable lpTask = () -> {
            byte response[] = httpObjectSocket.getNextQueuedMessage();
            ByteBuffer responseBuf = ByteBuffer.wrap(response);
            exchange.setResponseContentLength(response.length);
            if (response.length == 0) {
//                System.out.println("EE2");
                exchange.endExchange();
            } else {
                sinkchannel.getWriteSetter().set(
                    channel -> {
                        if (responseBuf.remaining() > 0)
                            try {
                                sinkchannel.write(responseBuf);
                                if (responseBuf.remaining() == 0) {
//                                    System.out.println("EE");
                                    exchange.endExchange();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                exchange.endExchange();
                            }
                        else {
//                            System.out.println("EE1");
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
                }
            }
        };
        if ( httpObjectSocket.getLongPollTask() != null ) {
            httpObjectSocket.triggerLongPoll();
        }
        httpObjectSocket.setLongPollTask(lpTask);
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        this.facade = facade;
        this.factory = factory;
    }

    @Override
    public IPromise closeServer() {
        return null;
    }


    public static class HTTPA extends Actor<HTTPA> {
        public IPromise hello(String s) {
            System.out.println(s+" received");
            return resolve("Hello "+s);
        }
    }

    public static void main( String a[] ) throws Exception {
        Pair<PathHandler, Undertow> serverPair = UndertowWebsocketServerConnector.GetServer(8080, "localhost");
        HTTPA facade = Actors.AsActor(HTTPA.class);
        UndertowHttpConnector con = new UndertowHttpConnector(facade);
        ActorServer actorServer = new ActorServer(con, facade, new Coding(SerializerType.MinBin));
        actorServer.start();
        serverPair.getFirst().addPrefixPath("http", con);

        Thread.sleep(100000000);

//        FSTConfiguration mbc = FSTConfiguration.createMinBinConfiguration();
//        Content content = Request.Post("http://localhost:8080/http/")
//            .bodyByteArray(mbc.asByteArray(new Object[]{"ruedi", "me"}))
//            .execute()
//            .returnContent();
//        String sessionId = (String) mbc.asObject(content.asBytes());
//        content = Request.Post("http://localhost:8080/http/"+sessionId)
//            .bodyByteArray(mbc.asByteArray(new Object[]{"ruedi", "me"}))
//            .execute()
//            .returnContent();
    }
}

package org.nustaq.kontraktor.remoting.http;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.websockets.UndertowWebsocketServerConnector;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.serialization.FSTConfiguration;
import org.xnio.ChannelListener;
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
        while ( path.startsWith("/") )
            path = path.substring(1);

        if ( path.trim().length() > 0 ) {
            String[] split = path.split("/");
            HttpObjectSocket httpObjectSocket = sessions.get(split[0]);
            if ( httpObjectSocket != null ) {

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
            ByteBuffer repsonseBuf = ByteBuffer.wrap(response);

            exchange.setResponseCode(200);
            exchange.setResponseContentLength(response.length);
            StreamSinkChannel sinkchannel = exchange.getResponseChannel();
            sinkchannel.getWriteSetter().set(
                channel -> {
                    if ( repsonseBuf.remaining() > 0 )
                        try {
                            sinkchannel.write(repsonseBuf);
                            if (repsonseBuf.remaining() == 0) {
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

    }

    public static void main( String a[] ) throws IOException, InterruptedException {
        Pair<PathHandler, Undertow> serverPair = UndertowWebsocketServerConnector.GetServer(8080, "localhost");
        UndertowHttpConnector con = new UndertowHttpConnector(Actors.AsActor(HTTPA.class));
        serverPair.getFirst().addPrefixPath("http", con);

        Thread.sleep(1000);

        FSTConfiguration mbc = FSTConfiguration.createMinBinConfiguration();
        Content content = Request.Post("http://localhost:8080/http/")
            .bodyByteArray(mbc.asByteArray(new Object[]{"ruedi", "me"}))
            .execute()
            .returnContent();
        String sessionId = (String) mbc.asObject(content.asBytes());
        content = Request.Post("http://localhost:8080/http/"+sessionId)
            .bodyByteArray(mbc.asByteArray(new Object[]{"ruedi", "me"}))
            .execute()
            .returnContent();
    }
}

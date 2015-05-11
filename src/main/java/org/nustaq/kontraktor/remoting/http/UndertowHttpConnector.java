package org.nustaq.kontraktor.remoting.http;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * Created by ruedi on 12.05.2015.
 *
 * A longpoll based connector. Only MinBin coding using POST requests is supported
 *
 */
public class UndertowHttpConnector implements ActorServerConnector, HttpHandler {

    Actor facade;

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
                    facade.execute( () -> requestReceived( exchange, buf.array(), rpath ) );
                }
            }
        );
    }

    protected void requestReceived( HttpServerExchange exchange, byte[] postData, String path) {

    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {

    }

    @Override
    public IPromise closeServer() {
        return null;
    }

}

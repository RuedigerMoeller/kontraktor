package org.nustaq.kontraktor.undertow.websockets;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.undertow.http.KRestProcessorAdapter;
import org.nustaq.kontraktor.undertow.http.KUTReq;
import org.nustaq.kontraktor.util.Log;
import org.xnio.Buffers;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruedi on 27/03/15.
 */
public class KUndertowWebSocketHandler extends WebSocketProtocolHandshakeHandler implements HttpHandler {

    WebSocketActorServerAdapter endpoint;
    WSLongPollFallbackHandler longPollFallbackHandler;

    public static class WithResult {

        public WithResult(WebSocketConnectionCallback cb, KUndertowWebSocketHandler handler) {
            this.cb = cb;
            this.handler = handler;
        }

        public WebSocketConnectionCallback cb;
        public KUndertowWebSocketHandler handler;
    }
    public static WithResult With(WebSocketActorServerAdapter endpoint) {
        KUndertowWebSocketHandler handler[] = {null};
        WebSocketConnectionCallback cb = (ex, ch) -> handler[0].doConnect(ex, ch);
        handler[0] = new KUndertowWebSocketHandler(endpoint, cb, new WSLongPollFallbackHandler());
        return new WithResult(cb,handler[0]);
    }

    public WSLongPollFallbackHandler getLongPollFallbackHandler() {
        return longPollFallbackHandler;
    }

    protected KUndertowWebSocketHandler(WebSocketActorServerAdapter endpoint, WebSocketConnectionCallback cb, WSLongPollFallbackHandler lpHandler) {
        super(cb, lpHandler);
        this.endpoint = endpoint;
        lpHandler.endpoint = endpoint;
        longPollFallbackHandler = lpHandler;
    }

    public static class WSLongPollFallbackHandler implements HttpHandler {

        WebSocketActorServerAdapter endpoint;

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if ( exchange.getRequestMethod() != Methods.POST )
            {
                Log.Warn(this,"only post supported for long poll requests");
                exchange.setResponseCode(404);
                exchange.endExchange();
                return;
            }
            KUTReq req = new KUTReq(exchange);
            StreamSourceChannel requestChannel = exchange.getRequestChannel();
            String first = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
            int len = Integer.parseInt(first);
            // read post data. FIXME: this is blocking (??). need to handle this async ?
            long tim = System.nanoTime();
            ByteBuffer buf = ByteBuffer.allocate(len);
            while ( buf.remaining() > 0 ) {
                if ( requestChannel.read(buf) < 0 ) {
                    Log.Warn(this, "failed to read " + len + " bytes from request");
                }
            }
//            System.out.println("post read poll "+(System.nanoTime()-tim));
            req.setBinaryContent(buf.array());
            exchange.dispatch();
            endpoint.handleLongPoll(req).then( bytarr -> {
                exchange.setResponseCode(200);
                exchange.getResponseHeaders().add( new HttpString("Access-Control-Allow-Origin"), "*" );
                try {
                    if ( bytarr != null && bytarr.length > 0 )
                        KRestProcessorAdapter.writeBlocking(exchange.getResponseChannel(), bytarr);
                    exchange.endExchange();
                } catch (IOException e) {
                    Log.Warn(this,e);
                    exchange.endExchange();
                }
            }).catchError(error -> {
                exchange.setResponseCode(500);
                exchange.endExchange();
            });
        }
    }

    public void doConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        UndertowChannelAdapter utChannel = new UndertowChannelAdapter(channel);
        channel.setAttribute("utchannel",utChannel);

        endpoint.onOpen(utChannel);
        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                endpoint.onTextMessage( utChannel, message.getData() );
            }

            @Override
            protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                ByteBuffer[] data = message.getData().getResource();
                endpoint.onBinaryMessage( utChannel, getBytes(data) );
            }

            @Override
            protected void onFullPingMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullPingMessage(channel, message);
            }

            @Override
            protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                endpoint.onPong( utChannel );
            }

            @Override
            protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullCloseMessage(channel,message);
            }

            @Override
            protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                endpoint.onClose( utChannel );
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
                endpoint.onError( utChannel );
            }
        });
        channel.resumeReceives();
    }

    private byte[] getBytes(ByteBuffer[] data) {
        return Buffers.take(data,0,data.length);
    }

}

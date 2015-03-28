package org.nustaq.kontraktor.undertow;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.nustaq.kontraktor.Actor;

import java.io.IOException;
import java.util.function.Function;

/**
 * Created by ruedi on 27/03/15.
 */
public class KWebSocketHandler extends WebSocketProtocolHandshakeHandler implements WebSocketConnectionCallback {

    private final Function<WebSocketChannel, ? extends KHttpSession> factory;

    public KWebSocketHandler(WebSocketConnectionCallback callback, Function<WebSocketChannel,? extends KHttpSession> factory) {
        super(callback);
        this.factory = factory;
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        KHttpSession session = factory.apply(channel);
        channel.setAttribute("wssession",session);

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                super.onFullTextMessage(channel, message);
            }

            @Override
            protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullBinaryMessage(channel, message);
            }

            @Override
            protected void onFullPingMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullPingMessage(channel, message);
            }

            @Override
            protected void onFullPongMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullPongMessage(channel, message);
            }

            @Override
            protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                super.onFullCloseMessage(channel, message);
            }

            @Override
            protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                super.onCloseMessage(cm, channel);
            }

            @Override
            protected void onError(WebSocketChannel channel, Throwable error) {
                super.onError(channel, error);
            }
        });
        channel.resumeReceives();
    }

}

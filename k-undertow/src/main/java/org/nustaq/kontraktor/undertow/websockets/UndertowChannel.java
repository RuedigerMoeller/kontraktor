package org.nustaq.kontraktor.undertow.websockets;

import io.undertow.websockets.core.WebSocketChannel;
import org.nustaq.kontraktor.remoting.http.websocket.WebSocketCchannelAdapter;

/**
 * Created by ruedi on 28/03/15.
 */
public class UndertowChannel implements WebSocketCchannelAdapter {
    private final WebSocketChannel channel;

    public UndertowChannel(WebSocketChannel channel) {
        this.channel = channel;
    }
}

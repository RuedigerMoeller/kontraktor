package org.nustaq.kontraktor.remoting.websocket.adapter;

import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;

/**
 * Created by ruedi on 29/03/15.
 */
public class WebSocketErrorMessage {

    final WebSocketChannelAdapter channel;

    public WebSocketErrorMessage(WebSocketChannelAdapter channel) {
        this.channel = channel;
    }

    public WebSocketChannelAdapter getChannel() {
        return channel;
    }
}

package org.nustaq.kontraktor.remoting.http.websocket;

/**
 * Kontraktor uses binary messages to enable transparent communication,
 * however in case a client sends test messages,
 * the receiving actor will receive them via the generic $receive() message present
 * on any actor
 */
public class WebSocketTextMessage {
    private final WebSocketChannelAdapter channel;
    private final String msg;

    public WebSocketTextMessage(String msg, WebSocketChannelAdapter channel) {
        this.msg = msg;
        this.channel = channel;
    }

    public WebSocketChannelAdapter getChannel() {
        return channel;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "WebSocketTextMessage{" +
                   "msg='" + msg + '\'' +
                   '}';
    }
}

package org.nustaq.kontraktor.remoting.websocket.adapter;

/**
 * Created by ruedi on 28/03/15.
 */
public interface WebSocketChannelAdapter {

    void setAttribute(String key, Object value);

    Object getAttribute(String key);

    public void sendBinaryMessage(byte[] b);
    public void sendTextMessage(String s);
}

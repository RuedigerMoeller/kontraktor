package org.nustaq.kontraktor.remoting.websocket.adapter;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by ruedi on 28/04/15.
 *
 * In case no websocket connection could be establish, this serves a s dummy channel
 *
 */
public class LongPollSocketChannelAdapter implements WebSocketChannelAdapter {

    HashMap attrs = new HashMap();

    @Override
    public void setAttribute(String key, Object value) {
        attrs.put(key,value);
    }

    @Override
    public Object getAttribute(String key) {
        return attrs.get(key);
    }

    @Override
    public void sendBinaryMessage(byte[] b) {

    }

    @Override
    public void sendTextMessage(String s) {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}

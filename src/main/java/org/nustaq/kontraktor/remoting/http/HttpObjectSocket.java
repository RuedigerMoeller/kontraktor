package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by ruedi on 12.05.2015.
 *
 * bidirectional http longpoll based objectsocket backed by a binary queue.
 *
 */
public class HttpObjectSocket extends WebObjectSocket {

    long lastUse = System.currentTimeMillis();
    long creation = lastUse;
    String sessionId;
    BinaryQueue queue = new BinaryQueue(4096);
    ObjectSink sink;

    public HttpObjectSocket(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void updateTimeStamp() {
        lastUse = System.currentTimeMillis();
    }

    @Override
    public void sendBinary(byte[] message) {
        synchronized (this) {
            queue.addInt(message.length);
            queue.add( new HeapBytez(message) );
        }
    }

    @Override
    public void close() throws IOException {

    }

    public void setSink(ObjectSink sink) {
        this.sink = sink;
    }

    public ObjectSink getSink() {
        return sink;
    }

    public byte[] getNextQueuedMessage() {
        synchronized (this) {
            if ( queue.available() > 4 ) {
                int len = queue.readInt();
                return queue.readByteArray(len);
            } else {
                return new byte[0];
            }
        }
    }
}

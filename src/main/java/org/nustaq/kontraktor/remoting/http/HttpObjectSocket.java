package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
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
public class HttpObjectSocket implements ObjectSocket {

    String sessionId;
    FSTConfiguration conf;
    Throwable lastError;
    BinaryQueue queue = new BinaryQueue(4096);
    ObjectSink sink;

    public HttpObjectSocket(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        queue.add( new HeapBytez(conf.asByteArray(toWrite)) );
    }

    @Override
    public void flush() throws Exception {

    }

    @Override
    public void setLastError(Throwable ex) {
        lastError = ex;
    }

    @Override
    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public void setConf(FSTConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
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
}

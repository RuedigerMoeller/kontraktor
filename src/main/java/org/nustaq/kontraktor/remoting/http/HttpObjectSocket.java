package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.onheap.HeapBytez;

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

    Runnable longPollTask;

    Thread myThread;

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
        if ( myThread == null )
            myThread = Thread.currentThread();
        else if ( myThread != Thread.currentThread() )
            System.out.println("unexpected multithreading detected:"+myThread.getName()+" curr:"+Thread.currentThread().getName());
        queue.addInt(message.length);
        queue.add( new HeapBytez(message) );
        triggerLongPoll();
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
        if ( myThread == null )
            myThread = Thread.currentThread();
        else if ( myThread != Thread.currentThread() )
            System.out.println("unexpected multithreading detected:"+myThread.getName()+" curr:"+Thread.currentThread().getName());
        if ( queue.available() > 4 ) {
            int len = queue.readInt();
            return queue.readByteArray(len);
        } else {
            return new byte[0];
        }
    }

    public Runnable getLongPollTask() {
        return longPollTask;
    }

    public void triggerLongPoll() {
        longPollTask.run();
        longPollTask = null;
    }

    public void setLongPollTask(Runnable longPollTask) {
        this.longPollTask = longPollTask;
    }
}

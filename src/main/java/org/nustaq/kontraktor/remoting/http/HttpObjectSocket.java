package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.messagestore.HeapMessageStore;
import org.nustaq.kontraktor.remoting.base.messagestore.MessageStore;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.onheap.HeapBytez;

import java.io.IOException;

/**
 * Created by ruedi on 12.05.2015.
 *
 * bidirectional http longpoll based objectsocket backed by a binary queue.
 *
 */
public class HttpObjectSocket extends WebObjectSocket implements ObjectSink {

    public static int LP_TIMEOUT = 5_000;

    public static int REORDERING_HISTORY_SIZE = 5; // amx size of recovered gaps on receiver side

    final Runnable closeAction;
    long lastUse = System.currentTimeMillis();
    long creation = lastUse;
    String sessionId;
    BinaryQueue queue = new BinaryQueue(4096);
    ObjectSink sink;
    MessageStore store = new HeapMessageStore(REORDERING_HISTORY_SIZE);
    Runnable longPollTask;
    Thread myThread;
    long longPollTaskTime;

    public HttpObjectSocket(String sessionId, Runnable closeAction ) {
        this.sessionId = sessionId;
        this.closeAction = closeAction;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void updateTimeStamp() {
        lastUse = System.currentTimeMillis();
    }

    public long getLastUse() {
        return lastUse;
    }

    @Override
    public void sendBinary(byte[] message) {
        checkThread();
        queue.addInt(sendSequence.get());
        queue.addInt(message.length);
        queue.add( new HeapBytez(message) );
        triggerLongPoll();
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        checkThread();
        super.writeObject(toWrite);
    }

    @Override
    public void close() throws IOException {
        if ( closeAction != null ) {
            closeAction.run();
        }
    }

    public void setSink(ObjectSink sink) {
        this.sink = sink;
    }

    public ObjectSink getSink() {
        return this;
    }

    public Pair<byte[],Integer> getNextQueuedMessage() {
        checkThread();
        if ( queue.available() > 8 ) {
            int seq = queue.readInt();
            int len = queue.readInt();
            if ( len>0 && queue.available() >= len )
                return new Pair(queue.readByteArray(len),seq);
            else
                return new Pair(new byte[0],0);
        } else {
            return new Pair(new byte[0],0);
        }
    }

    protected void checkThread() {
        if ( myThread == null )
            myThread = Thread.currentThread();
        else if ( myThread != Thread.currentThread() )
            System.out.println("unexpected multithreading detected:"+myThread.getName()+" curr:"+Thread.currentThread().getName());
    }

    public Runnable getLongPollTask() {
        return longPollTask;
    }

    public void triggerLongPoll() {
        checkThread();
        if (longPollTask!=null) {
            longPollTask.run();
            longPollTask = null;
        }
    }

    public void setLongPollTask(Runnable longPollTask) {
        checkThread();
        this.longPollTask = longPollTask;
        this.longPollTaskTime = System.currentTimeMillis();
    }

    public long getLongPollTaskTime() {
        return longPollTaskTime;
    }

    /////////////// add reordering support for sink

    int lastSinkSequence = 0; // fixme: check threading

    @Override
    public void receiveObject(Object received) {
        sink.receiveObject(received);
    }

    @Override
    public void sinkClosed() {
        sink.sinkClosed();
    }

    @Override
    public int getLastSinkSequence() {
        return lastSinkSequence;
    }

    @Override
    public void setLastSinkSequence(int ls) {
        lastSinkSequence = ls;
    }

    @Override
    public Object takeStoredMessage(int seq) {
        return store.getMessage("rec",seq);
    }

    @Override
    public void storeGappedMessage(int inSequence, Object response) {
        store.putMessage("rec", inSequence, response);
    }

    public Object takeStoredLPMessage(int seq) {
        return store.getMessage("sen",seq);
    }

    public void storeLPMessage(int inSequence, Object msg) {
        store.putMessage("sen", inSequence, msg);
    }

}

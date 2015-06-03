package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.messagestore.HeapMessageStore;
import org.nustaq.kontraktor.remoting.base.messagestore.MessageStore;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by ruedi on 12.05.2015.
 *
 * bidirectional http longpoll based objectsocket backed by a binary queue.
 *
 */
public class HttpObjectSocket extends WebObjectSocket implements ObjectSink {

    public static int LP_TIMEOUT = 15_000;
    public static int HISTORY_SIZE = 3; // max size of recovered polls on server sides
    public static int HTTP_BATCH_SIZE = 500; // batch messages to partially make up for http 1.1 synchronous design failure

    final Runnable closeAction;
    long lastUse = System.currentTimeMillis();
    long creation = lastUse;
    String sessionId;
    BinaryQueue queue = new BinaryQueue(4096);
    ObjectSink sink;
    MessageStore store = new HeapMessageStore(HISTORY_SIZE);
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
        synchronized (queue) {
            queue.addInt(sendSequence.get());
            queue.addInt(message.length);
            queue.add( new HeapBytez(message) );
            triggerLongPoll();
        }
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        synchronized (queue) {
            super.writeObject(toWrite);
        }
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
        synchronized (queue) {
            if ( queue.available() < 8 ) {
                try {
                    flush();
                } catch (Exception e) {
                    FSTUtil.rethrow(e);
                }
            }
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
    }

    protected void checkThread() {
        if ( myThread == null )
            myThread = Thread.currentThread();
        else if ( myThread != Thread.currentThread() ) {
            System.out.println("unexpected multithreading detected:"+myThread.getName()+" curr:"+Thread.currentThread().getName());
            Thread.dumpStack();
        }
    }

    public Runnable getLongPollTask() {
        return longPollTask;
    }

    public void triggerLongPoll() {
        if (longPollTask!=null) {
            longPollTask.run();
            longPollTask = null;
        }
    }

    public void setLongPollTask(Runnable longPollTask) {
        this.longPollTask = longPollTask;
        this.longPollTaskTime = System.currentTimeMillis();
    }

    public long getLongPollTaskTime() {
        return longPollTaskTime;
    }

    @Override
    public void receiveObject(ObjectSink asink, Object received, List<IPromise> createdFutures) {
        sink.receiveObject(asink,received, createdFutures);
    }

    @Override
    public void sinkClosed() {
        sink.sinkClosed();
    }

    public Object takeStoredLPMessage(int seq) {
        return store.getMessage("sen",seq);
    }

    public void storeLPMessage(int inSequence, Object msg) {
        store.putMessage("sen", inSequence, msg);
    }

}

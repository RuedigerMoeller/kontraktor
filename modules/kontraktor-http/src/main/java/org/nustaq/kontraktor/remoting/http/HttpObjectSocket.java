/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.messagestore.HeapMessageStore;
import org.nustaq.kontraktor.remoting.base.messagestore.MessageStore;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 12.05.2015.
 *
 * bidirectional http longpoll based objectsocket backed by a binary queue.
 *
 */
public class HttpObjectSocket extends WebObjectSocket implements ObjectSink {
    private static final boolean LP_DEBUG = false;
    static AtomicInteger idCount = new AtomicInteger(0);
    int id = idCount.incrementAndGet();

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
    volatile Pair<Runnable, KHttpExchange> longPollTask;
    Thread myThread;
    volatile long longPollTaskTime;

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
        queue.add(new HeapBytez(message));
        if ( LP_DEBUG )
            System.out.println("send binary " + sendSequence.get());
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

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getConnectionIdentifier() {
        return sessionId;
    }

    public void setSink(ObjectSink sink) {
        this.sink = sink;
    }

    public ObjectSink getSink() {
        return this;
    }

    public Pair<byte[],Integer> getNextQueuedMessage() {
        checkThread();
//            if ( queue.available() < 8 )
        {
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

    protected void checkThread() {
        if ( myThread == null )
            myThread = Thread.currentThread();
        else if ( myThread != Thread.currentThread() ) {
            Log.Error(this,"unexpected multithreading detected:" + myThread.getName() + " curr:" + Thread.currentThread().getName());
            Thread.dumpStack();
        }
    }

    public Pair<Runnable,KHttpExchange> getLongPollTask() {
        return longPollTask;
    }

    public void cancelLongPoll() {
        synchronized (this) {
            if (longPollTask!=null) {
                longPollTask.cdr().endExchange();
                longPollTask = null;
            }
        }
    }

    AtomicInteger triggerPending = new AtomicInteger(0);
    public void triggerLongPoll() {
        synchronized (this) {
            if (longPollTask!=null) {
                if ( LP_DEBUG )
                    System.out.println("SEND PENDING "+triggerPending);
                if (triggerPending.get() > 0) // fixme: concurrency bug
                    triggerPending.decrementAndGet();
                Runnable car = longPollTask.car();
                longPollTask = null;
                car.run();
            } else {
                if ( LP_DEBUG )
                    System.out.println("INC PENDING "+triggerPending);
                triggerPending.incrementAndGet();
            }
        }
    }

    public void setLongPollTask(Pair<Runnable, KHttpExchange> longPollTask) {
        synchronized (this) {
            this.longPollTask = longPollTask;
            this.longPollTaskTime = System.currentTimeMillis();
            if ( LP_DEBUG )
                System.out.println("SET LONG POLL"+triggerPending);
            if ( triggerPending.get() > 0 ) {
                triggerLongPoll();
            }
        }
    }

    public long getLongPollTaskTime() {
        return longPollTaskTime;
    }

    @Override
    public void receiveObject(ObjectSink asink, Object received, List<IPromise> createdFutures, Object securityContext) {
        sink.receiveObject(asink,received, createdFutures, securityContext);
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

    @Override
    public void flush() throws Exception {
        if (objects.size() == 0) {
            return;
        }
        objects.add(sendSequence.incrementAndGet()); // sequence
        Object[] objArr = objects.toArray();
        objects.clear();
        sendBinary(conf.asByteArray(objArr));
    }

}

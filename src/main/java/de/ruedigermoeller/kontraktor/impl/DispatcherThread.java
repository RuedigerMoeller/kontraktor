package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Date: 03.01.14
 * Time: 22:19
 * To change this template use File | Settings | File Templates.
 */

/**
 * Implements the default dispatcher/scheduling of actors.
 * For each actor created from "outside" (not from within another actor). A new DispatcherThread is created
 * automatically. An actor created from within another actor inherits the dispatcher of the enclosing actor
 * by default.
 * Calls from actors sharing the same dispatcher are done directly (no queueing). Calls across actors in
 * different dispatchers are put to the Channel of the receiving dispatcher. Note that cross-dispatcher calls
 * are like 1000 times slower than inbound calls.
 *
 * Each dispatcher owns exactly one single thread.
 * Note that dispatchers must be terminated if not needed any longer, as a thread is associated with them.
 *
 * For more sophisticated applications it might be appropriate to manually set up dispatchers (Actors.newDispatcher()).
 * The Actors.Channel method allows to specifiy a dedicated dispatcher on which to run the actor. This way it is possible
 * to exactly balance and control the number of threads created and which thread operates a set of actors.
 *
 */
public class DispatcherThread extends Thread {

    public static AtomicInteger instanceCount = new AtomicInteger(0);

    /**
     * defines the strategy applied when idling
     */
    public static BackOffStrategy backOffStrategy = new BackOffStrategy();

    AtomicInteger usingActors = new AtomicInteger(0);
    volatile
    boolean shutDown = false;
    private boolean dead;

    public static int DEFAULT_QUEUE_SIZE = 30000;
    Queue queue;
    Queue cbQueue;
    int instanceNum;

    class CallbackInvokeHandler implements InvocationHandler {

        final Object target;

        public CallbackInvokeHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ( target != null )
                return dispatchCallback(target, method, args);
            return null;
        }
    }

    String stack; // contains stacktrace of creation of this

    public DispatcherThread() {
        init(DEFAULT_QUEUE_SIZE);
    }

    public DispatcherThread(int qSize) {
        init(qSize);
    }

    public boolean isEmpty() {
        return queue.isEmpty()&&cbQueue.isEmpty();
    }

    public InvocationHandler getInvoker(Object toWrap) {
        return new CallbackInvokeHandler(toWrap);
    }

    protected void init(int qSize) {
        if (qSize<=0)
            qSize = DEFAULT_QUEUE_SIZE;
        queue = new MpscConcurrentQueue<CallEntry>(qSize);
        cbQueue = new MpscConcurrentQueue<CallEntry>(qSize);
        instanceNum = instanceCount.incrementAndGet();
        StringWriter stringWriter = new StringWriter(1000);
        PrintWriter s = new PrintWriter(stringWriter);
        new Exception().printStackTrace(s);
        s.flush();
        stack = stringWriter.getBuffer().toString();
        setName("ActorDisp spawned from ["+Thread.currentThread().getName()+"] "+System.identityHashCode(this));
        start();
    }

    @Override
    public String toString() {
        return "DispatcherThread{" +
                " name:"+getName()+
                '}';
    }

    public void actorAdded(Actor a) {
        usingActors.incrementAndGet();
    }

    public void actorStopped(Actor a) {
        final int count = usingActors.decrementAndGet();
        if ( count == 0 ) {
            shutDown();
        }
    }

    /**
     * @return true if blocked and polling channels should be done
     */
    public boolean dispatchOnObject( CallEntry entry ) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        if ( ! queue.offer(entry) ) {
            return true;
        }
        return false;
    }

    public boolean dispatchCallback( Object callback, Method method, Object args[]) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        CallEntry e = new CallEntry(callback, method, args, false);
        DispatcherThread sender = getThreadDispatcher();
        if ( ! cbQueue.offer(e) ) {
            return true;
        }
        return false;
    }

    public static DispatcherThread getThreadDispatcher() {
        DispatcherThread sender = null;
        if ( Thread.currentThread() instanceof DispatcherThread)
            sender = (DispatcherThread) Thread.currentThread();
        return sender;
    }

    /**
     * poll queues until callentry is received, then exit
     * @param e
     */
    // FIXME: add timeout
    public Object yieldPoll(CallEntry e) {
        int emptyCount = 0;
        boolean isShutDown = false;
        while( ! isShutDown ) {

            if ( pollQs() ) {
                emptyCount = 0;
            }
            else {
                emptyCount++;
                DispatcherThread.this.yield(emptyCount);
                if (shutDown) // access volatile only when idle
                    isShutDown = true;
            }
            if ( e.isAnswered() ) {
                Object result = e.getResult();
                if ( result instanceof Throwable ) {
                    throw new RuntimeException((Throwable) result);
                }
                return result;
            }
        }
        return null;
    }

    public void run() {
        int emptyCount = 0;
        boolean isShutDown = false;
        while( ! isShutDown ) {

            if ( pollQs() ) {
                emptyCount = 0;
            }
            else {
                emptyCount++;
                DispatcherThread.this.yield(emptyCount);
                if (shutDown) // access volatile only when idle
                    isShutDown = true;
            }
        }
        dead = true;
        instanceCount.decrementAndGet();
        System.out.println("dispatcher finished");
    }

    // return true if msg was avaiable
    public boolean pollQs() {
        CallEntry poll = (CallEntry) cbQueue.poll();
        if (poll==null)
            poll = (CallEntry) queue.poll();
        if ( poll != null ) {
            try {
                Object invoke = poll.getMethod().invoke(poll.getTarget(), poll.getArgs());
                if ( poll.isYield() ) {
                    poll.setResult(invoke);
                }
                return true;
            } catch (Exception e) {
                poll.setResult(e);
                if ( e.getCause() != null )
                    e.getCause().printStackTrace();
                else
                    e.printStackTrace();
            }
        }
        return false;
    }

    public static void yield(int count) {
        backOffStrategy.yield(count);
    }

    public int getQSize() {
        return queue.size()+cbQueue.size(); // FIXME: bad for concurrentlinkedq
    }

    /**
     * @return true if DispatcherThread is not shut down
     */
    public boolean isShutDown() {
        return ! shutDown;
    }

    /**
     * terminate operation after emptying Q
     */
    public void shutDown() {
        shutDown=true;
    }

    /**
     * terminate operation immediately. Pending messages in Q are lost
     */
    public void shutDownImmediate() {
        throw new RuntimeException("unimplemented");
    }

    /**
     * blocking method, use for debugging only.
     */
    public void waitEmpty(long nanos) {
        while( ! isEmpty() )
            LockSupport.parkNanos(nanos);
    }

}

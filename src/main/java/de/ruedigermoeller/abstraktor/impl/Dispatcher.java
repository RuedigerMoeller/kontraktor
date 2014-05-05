package de.ruedigermoeller.abstraktor.impl;

import de.ruedigermoeller.abstraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 03.01.14
 * Time: 22:19
 * To change this template use File | Settings | File Templates.
 */

/**
 * Implements the default dispatcher/scheduling of actors.
 * For each actor created from "outside" (not from within another actor). A new Dispatcher is created
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
public class Dispatcher extends Thread {

    public static AtomicInteger instanceCount = new AtomicInteger(0);

    //volatile
    boolean shutDown = false;
    private boolean dead;

    final int DEFAULT_QUEUE_SIZE = 10000;
    Queue queue = new MpscConcurrentQueue<CallEntry>(DEFAULT_QUEUE_SIZE);
//    Queue queue = new ConcurrentLinkedDeque();
    int instanceNum;

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    protected static class CallEntry {
        final private Object target;
        final private Method method;
        final private Object[] args;

        CallEntry(Object actor, Method method, Object[] args) {
            this.target = actor;
            this.method = method;
            this.args = args;
        }

        public Object getTarget() {
            return target;
        }
        public Method getMethod() {
            return method;
        }
        public Object[] getArgs() { return args; }
    }

    String stack;

    public Dispatcher() {
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
        return "Dispatcher{" +
                " name:"+getName()+
                '}';
    }

    /**
     *
     * @param actorRef - receiver of call
     * @param method
     * @param args
     * @return true if blocked and polling channels should be done
     */
    public boolean dispatch( ActorProxy actorRef, Method method, Object args[]) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if ( arg instanceof ActorFuture) {
                Dispatcher sender = getThreadDispatcher();
                args[i] = new ActorFutureWrapper<>(sender,(ActorFuture<Object>) arg);
            }
        }
        CallEntry e = new CallEntry(actorRef.getActor(), method, args);
        if ( ! queue.offer(e) ) {
            return true;
        }
        return false;
    }

    public boolean dispatchCallback( Object callback, Method method, Object args[]) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        CallEntry e = new CallEntry(callback, method, args);
        Dispatcher sender = getThreadDispatcher();
        if ( ! queue.offer(e) ) {
            return true;
        }
        return false;
    }

    public static Dispatcher getThreadDispatcher() {
        Dispatcher sender = null;
        if ( Thread.currentThread() instanceof Dispatcher )
            sender = (Dispatcher) Thread.currentThread();
        return sender;
    }

    public void run() {
        int emptyCount = 0;
        while( ! shutDown ) {

            if ( pollQs() ) {
                emptyCount = 0;
            }
            else {
                emptyCount++;
                Dispatcher.this.yield(emptyCount);
            }
        }
        dead = true;
        instanceCount.decrementAndGet();
    }

    // return true if msg was avaiable
    public boolean pollQs() {
        CallEntry poll = null;
        poll = (CallEntry) queue.poll();
        if ( poll != null ) {
            try {
                poll.getMethod().invoke(poll.getTarget(),poll.getArgs());
                return true;
            } catch (RuntimeException e) {
                if ( e.getCause() != null )
                    e.getCause().printStackTrace();
                else
                    e.printStackTrace();
//                throw e;
            } catch (InvocationTargetException e) {
                e.getCause().printStackTrace();
                throw new RuntimeException(e.getCause());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return false;
    }

    static int YIELD_THRESH = 100000;
    static int PARK_THRESH = YIELD_THRESH+50000;
    static int SLEEP_THRESH = PARK_THRESH+5000;
    public static void yield(int count) {
        if ( count > SLEEP_THRESH ) {
            LockSupport.parkNanos(1000*500);
        } else if ( count > PARK_THRESH ) {
            LockSupport.parkNanos(1);
        } else {
            if ( count > YIELD_THRESH)
                Thread.yield();
        }
    }

    public int getQSize() {
        return queue.size(); // FIXME: bad for concurrentlinkedq
    }

    /**
     * @return true if Dispatcher is not shut down
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

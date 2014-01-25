package de.ruedigermoeller.abstraktor.impl;

import de.ruedigermoeller.abstraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
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

    private int nested;

    boolean shutDown = false;
    private boolean dead;

    final int QS = 10000;
    Queue[] queue = {
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
    };

    Queue[] channels =  {
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
    };

    int instanceNum;
    public int qIndex = 2*(instanceCount.get()&(queue.length/2-1));

    public boolean isEmpty() {
        for (int i = 0; i < queue.length; i+=2) {
            if ( ! queue[i].isEmpty() )
                return false;
        }
        return true;
    }

    static class CallEntry {
        final private Actor target;
        final private Method method;
        final private Object[] args;
        final boolean sentinel;

        CallEntry(Actor actor, Method method, Object[] args, boolean sentinel) {
            this.target = actor;
            this.method = method;
            this.args = args;
            this.sentinel = sentinel;
        }

        public Actor getTarget() {
            return target;
        }
        public Method getMethod() {
            return method;
        }
        public Object[] getArgs() { return args; }
        public boolean isSentinel() { return sentinel; }
    }

    String stack;

    public Dispatcher() {
        instanceNum = instanceCount.incrementAndGet();
        StringWriter stringWriter = new StringWriter(1000);
        PrintWriter s = new PrintWriter(stringWriter);
        new Exception().printStackTrace(s);
        s.flush();
        stack = stringWriter.getBuffer().toString();
        setName("Dispatcher spawned from ["+Thread.currentThread().getName()+"]");
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
     * @param sameThread
     * @param method
     * @param args
     * @return true if blocked and polling channels should be done
     */
    public boolean dispatch( ActorProxy actorRef, boolean sameThread, Method method, Object args[]) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        CallEntry e = new CallEntry(actorRef.getActor(), method, args, false);
        Dispatcher sender = getThreadDispatcher();
        int qIndex = 0;
        if ( sender != null )
            qIndex = sender.qIndex;
        if ( actorRef.getActor().__isFIFO() ) {
            if ( ! queue[qIndex].offer(e) ) {
                return true;
            }
        } else {
            if ( ! channels[qIndex].offer(e) ) {
                return true;
            }
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

            if ( pollChannels() ) {
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
        boolean hadOne = false;
        for (int i = 0; i < queue.length; i+=2) {
            poll = (CallEntry) queue[i].poll();
            if ( poll != null ) {
                try {
                    poll.getMethod().invoke(poll.getTarget(),poll.getArgs());
                    hadOne = true;
                } catch (RuntimeException e) {
                    e.getCause().printStackTrace();
                    throw e;
                } catch (InvocationTargetException e) {
                    e.getCause().printStackTrace();
                    throw new RuntimeException(e.getCause());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return hadOne;
    }

    // return true if msg was avaiable
    public boolean pollChannels() {
        CallEntry poll = null;
        boolean hadOne = false;
        for (int i = 0; i < channels.length; i+=2) {
            poll = (CallEntry) channels[i].poll();
            if ( poll != null ) {
                try {
                    poll.getMethod().invoke(poll.getTarget(),poll.getArgs());
                    hadOne = true;
                } catch (RuntimeException e) {
                    e.getCause().printStackTrace();
                    throw e;
                } catch (InvocationTargetException e) {
                    e.getCause().printStackTrace();
                    throw new RuntimeException(e.getCause());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return hadOne;
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
        int queueSize = 0;
        for (int i = 0; i < queue.length; i+=2) {
            queueSize += queue[i].size();
        }
        return queueSize;
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
    public void waitEmpty() {
        while( ! isEmpty() )
            Thread.currentThread().yield();
    }

}

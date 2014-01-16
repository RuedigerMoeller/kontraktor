package de.ruedigermoeller.abstraktor.impl;

import de.ruedigermoeller.abstraktor.*;
import de.ruedigermoeller.abstraktor.sample.balancing.SubActor;
import de.ruedigermoeller.abstraktor.sample.balancing.WorkerActor;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * different dispatchers are put to the queue of the receiving dispatcher. Note that cross-dispatcher calls
 * are like 1000 times slower than inbound calls.
 *
 * Each dispatcher owns exactly one single thread.
 * Note that dispatchers must be terminated if not needed any longer, as a thread is associated with them.
 *
 * For more sophisticated applications it might be appropriate to manually set up dispatchers (Actors.newDispatcher()).
 * The Actors.New method allows to specifiy a dedicated dispatcher on which to run the actor. This way it is possible
 * to exactly balance and control the number of threads created and which thread operates a set of actors.
 *
 */
public class DefaultDispatcher implements Dispatcher {

    public static AtomicInteger instanceCount = new AtomicInteger(0);

    final int SENTINEL_MASK = 1024-1;

    Thread worker;
    private int nested;

    AtomicBoolean shutDown = new AtomicBoolean(false);
    private boolean dead;

    final int QS = 5000;
    Queue[] queue = {
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
            new MpscConcurrentQueue<CallEntry>(QS), null,
    };

    int instanceNum;
    boolean isSystemDispatcher;
    private int queueSize;
    public int qIndex = 2*(instanceCount.get()&(queue.length/2-1));

    public int getQueueSize() {
        return queueSize;
    }

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

    String name;
    String stack;

    public DefaultDispatcher() {
        instanceNum = instanceCount.incrementAndGet();
        start();
        StringWriter stringWriter = new StringWriter(1000);
        PrintWriter s = new PrintWriter(stringWriter);
        new Exception().printStackTrace(s);
        s.flush();
        stack = stringWriter.getBuffer().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        worker.setName(name);
    }

    @Override
    public String toString() {
        return "DefaultDispatcher{" +
                "worker=" + worker+" name:"+name+
                '}';
    }

    public boolean isSystemDispatcher() {
        return isSystemDispatcher;
    }

    public void setSystemDispatcher(boolean isSystemDispatcher) {
        this.isSystemDispatcher = isSystemDispatcher;
    }

    @Override
    public void dispatch( ActorProxy actorRef, boolean sameThread, Method method, Object args[]) {
        // MT. sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        CallEntry e = new CallEntry(actorRef.getActor(), method, args, false);
        int count = 0;
        DefaultDispatcher sender = (DefaultDispatcher) Actors.threadDispatcher.get();
        int qIndex = 0;
        if ( sender != null )
            qIndex = sender.qIndex;
        while ( ! queue[qIndex].offer(e) ) {
            if ( sender != null && sender.getNesting() < 2000 ) {
                boolean hadPoll;
                try {
                    sender.incNesting();
                    hadPoll = sender.poll();
                } finally {
                    sender.decNesting();
                }
                if ( ! hadPoll )
                    yield(count++);
                else
                    count = 0;
            } else {
                yield(count++);
            }
        }
    }

    public void start() {
        worker = new Thread() {
            public void run() {
                Actors.threadDispatcher.set(DefaultDispatcher.this);
                int emptyCount = 0;
                while( ! shutDown.get() ) {
                    if ( poll() ) {
                        emptyCount = 0;
                    }
                    else {
                        emptyCount++;
                        DefaultDispatcher.this.yield(emptyCount);
                    }
                }
                dead = true;
                instanceCount.decrementAndGet();
            }
        };
        worker.start();
    }

    // return true if msg was avaiable
    public boolean poll() {
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


    final int YIELD_THRESH = 100000;
    final int PARK_THRESH = YIELD_THRESH+50000;
    final int SLEEP_THRESH = PARK_THRESH+5000;
    protected void yield(int count) {
        if ( count > SLEEP_THRESH ) {
            LockSupport.parkNanos(1000*500);
        } else if ( count > PARK_THRESH ) {
            LockSupport.parkNanos(1);
        } else {
            if ( count > YIELD_THRESH)
                Thread.yield();
        }
    }

    public void calcQSize() {
        queueSize = 0;
        for (int i = 0; i < queue.length; i+=2) {
            queueSize += queue[i].size();
        }
    }

    public Thread getWorker() {
        return worker;
    }

    @Override
    public void incNesting() {
        nested++;
    }

    @Override
    public void decNesting() {
        nested--;
    }

    @Override
    public int getNesting() {
        return nested;
    }

    public Marshaller instantiateMarshaller(Actor target) {
        return new Marshaller() {
            HashMap<String, Method> methodCache = new HashMap<>();

            @Override
            public boolean isSameThread(String methodName, ActorProxy proxy) {
                return proxy.getDispatcher().getWorker() == Thread.currentThread();
            }

            @Override
            /**
             * callback from bytecode weaving
             */
            public boolean doDirectCall(String methodName, ActorProxy proxy) {
                return proxy.getActor().__outCalls == 0;
            }

            @Override
            public void dispatchCall( ActorProxy senderRef, boolean sameThread, String methodName, Object args[] ) {
                // here sender + receiver are known in a ST context
                Method method = methodCache.get(methodName);
                Actor actor = senderRef.getActor();
                if ( method == null ) {
                    Method[] methods = actor.getClass().getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        Method m = methods[i];
                        if ( m.getName().equals(methodName) ) {
                            methodCache.put(methodName,m);
                            method = m;
                            break;
                        }
                    }
                }
                actor.getDispatcher().dispatch(senderRef, sameThread, method, args);
            }
        };
    }

    /**
     * stop processing messages, but do not block adding of new messages to Q
     */
    @Override
    public void pauseOperation() {
        throw new RuntimeException("unimplemented");
    }

    /**
     * continue processing messages
     */
    @Override
    public void continueOperation() {
        throw new RuntimeException("unimplemented");
    }

    /**
     * @return true if Dispatcher is not shut down
     */
    @Override
    public boolean isAlive() {
        return ! shutDown.get();
    }

    /**
     * @return true if Dispatcher is paused but not shut-down
     */
    @Override
    public boolean isPaused() {
        return false;
    }

    /**
     * terminate operation after emptying Q
     */
    @Override
    public void shutDown() {
        shutDown.set(true);
    }

    /**
     * terminate operation immediately. Pending messages in Q are lost
     */
    @Override
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

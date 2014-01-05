package de.ruedigermoeller.abstractor.impl;

import de.ruedigermoeller.abstractor.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * current limits (future work/possible extensions):
 * - Currently no efforts are done to implement dynamic rebalancing (e.g. move actors to other dispatchers in case
 *   of high load).
 *
 * - The DefaultDispatcher operates of an unbounded ConcurrentLinkedQueue, there are faster lockfree implementations
 *   out there.
 *
 * - flow control is probably even more important than
 *
 * Creating your own dispatcher logic/implementation:
 * - getMarshhaller is important, as it defines wether to send a message directly, queued or .. (block, remote, etc).
 *   note currently the sender is unknown, but could be set using threadlocals in case
 * - in order to keep inheritance of dispatchers when creating actors in actors, the Actors.threadDispatcher must
 *   be set. (see start() method below)
 */
public class DefaultDispatcher implements Dispatcher {

    public static AtomicInteger instanceCount = new AtomicInteger(0);

    Thread worker;
    ConcurrentLinkedQueue<CallEntry> queue = new ConcurrentLinkedQueue<>();
    AtomicBoolean shutDown = new AtomicBoolean(false);
    private volatile boolean dead;

    static class CallEntry {
        private Actor target;
        private Method method;
        private Object[] args;

        CallEntry(Actor actor, Method method, Object[] args) {
            this.target = actor;
            this.method = method;
            this.args = args;
        }

        public Actor getTarget() {
            return target;
        }
        public Method getMethod() {
            return method;
        }
        public Object[] getArgs() { return args; }
    }

    public DefaultDispatcher() {
        instanceCount.incrementAndGet();
        start();
    }

    @Override
    public void dispatch(Actor actor, Method method, Object args[]) {
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher ");
        queue.offer(new CallEntry(actor,method,args));
        Thread.yield();
    }

    public void start() {
        worker = new Thread("ActorDispatcher") {
            public void run() {
                Actors.threadDispatcher.set(DefaultDispatcher.this);
                int emptyCount = 0;
                while( ! shutDown.get() ) {
                    if ( poll() )
                        emptyCount = 0;
                    else
                        emptyCount++;
                    DefaultDispatcher.this.yield(emptyCount);
                }
                dead = true;
                instanceCount.decrementAndGet();
            }
        };
        worker.start();
    }

    protected void yield(int count) {
        if ( count > 105000 ) {
            LockSupport.parkNanos(1000000);
        } else if ( count > 100000 ) {
            LockSupport.parkNanos(1);
        } else if ( count > 100 )
            Thread.yield();
    }

    protected boolean poll() {
        CallEntry poll = queue.poll();
        if ( poll != null ) {
            try {
                poll.getMethod().invoke(poll.getTarget(),poll.getArgs());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public Thread getWorker() {
        return worker;
    }

    public Marshaller instantiateMarshaller(Actor target) {
        return new Marshaller() {
            HashMap<String, Method> methodCache = new HashMap<>();

            @Override
            /**
             * callback from bytecode weaving
             */
            public boolean doDirectCall(String methodName, ActorProxy proxy) {
                return proxy.getDispatcher().getWorker() == Thread.currentThread();
            }

            @Override
            public void dispatchCall( Actor actor, String methodName, Object args[] ) {
                Method method = methodCache.get(methodName);
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
                actor.getDispatcher().dispatch(actor,method,args);
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
        while( queue.peek() != null )
            Thread.currentThread().yield();
    }


}

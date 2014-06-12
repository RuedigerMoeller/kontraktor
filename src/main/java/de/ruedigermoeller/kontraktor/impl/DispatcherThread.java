package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;
import de.ruedigermoeller.kontraktor.annotations.CallerSideMethod;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
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

    // FIXME: static stuff to be moved to instance, hold a scheduler with each actor. reuqires a scheduler to be split from dispatcherthread

    public static AtomicInteger instanceCount = new AtomicInteger(0);
    public static int DEFAULT_QUEUE_SIZE = 30000;

    public static Future Put2QueuePolling(CallEntry e) {
        final Future fut;
        if (e.hasFutureResult()) {
            fut = new Promise();
            e.setFutureCB(new CallbackWrapper( e.getTargetActor() ,new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    fut.receiveResult(result,error);
                }
            }));
        } else
            fut = null;
        Put2QueuePolling(e.getTargetActor().__mailbox,e);
        return fut;
    }

    public static void Put2QueuePolling(Queue q, Object o) {
        int count = 0;
        while ( ! q.offer(o) ) {
            yield(count++);
        }
    }

    static ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    static Method getCachedMethod(String methodName, Actor actor) {
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
        return method;
    }


    public static Object DispatchCall( Actor sendingActor, Actor receiver, String methodName, Object args[] ) {
        // System.out.println("dispatch "+methodName+" "+Thread.currentThread());
        // here sender + receiver are known in a ST context
        Actor actor = receiver.getActor();
        Method method = getCachedMethod(methodName, actor);

        int count = 0;
        // scan for callbacks in arguments ..
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if ( arg instanceof Callback) {
                args[i] = new CallbackWrapper<>(receiver,(Callback<Object>) arg);
            }
        }

        CallEntry e = new CallEntry(
                actor,
                method,
                args,
                actor
        );
        if ( receiver.__isSeq ) {
            sendingActor.methodSequence.get().add(e);
            return null;
        }
        return Put2QueuePolling(e);
    }


    /**
     * defines the strategy applied when idling
     */
    ArrayList<Actor> queueList = new ArrayList<>();
    Queue queues[] = new Queue[0];
    Queue cbQueues[]= new Queue[0];

    protected int instanceNum;
    static protected BackOffStrategy backOffStrategy = new BackOffStrategy(); // FIXME: should not be static

    protected boolean shutDown = false;
    protected int defQSize;

    public DispatcherThread() {
    }

    public DispatcherThread(int qSize) {
        init(qSize);
    }

    public void init(int qSize) {
        initNoStart(qSize);
        start();
    }

    public void initNoStart(int qSize) {
        if (qSize<=0)
            qSize = DEFAULT_QUEUE_SIZE;
        defQSize = qSize;
        instanceNum = instanceCount.incrementAndGet();
        setName("ActorDisp spawned from [" + Thread.currentThread().getName() + "] " + System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return "DispatcherThread{" +
                " name:"+getName()+
                '}';
    }

    public void actorAdded(Actor a) {
        synchronized (queueList) {
            queueList.add(a);
        }
    }

    public void actorStopped(Actor a) {
        synchronized (queueList) {
            queueList.remove(a);
        }
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
        instanceCount.decrementAndGet();
    }

    // if list of queues to schedule has changed,
    // apply the change. needs to be done in thread
    private void applyQueueList() {
        synchronized (queueList) {
            queues = new Queue[queueList.size()];
            cbQueues = new Queue[queueList.size()];
            for (int i = 0; i < queues.length; i++) {
                queues[i] = queueList.get(i).__mailbox;
                cbQueues[i] = queueList.get(i).__cbQueue;
            }
        }
    }

    // poll all queues in queue arr round robin
    int count = 0;
    protected CallEntry pollQueues(Queue[] cbQueues, Queue[] queueArr) {
        if ( count >= queueArr.length ) {
            // check for changed queueList each run FIXME: too often !
            count = 0;
            if ( queueArr.length != queueList.size() ) {
                applyQueueList();
            }
            if ( queueArr.length == 0 ) {
                return null;
            }
        }
        CallEntry res = (CallEntry) cbQueues[count].poll();
        if ( res == null )
            res = (CallEntry) queueArr[count].poll();
        count++;
        return res;
    }


    // return true if msg was avaiable
    int profileCounter = 0;
    int loadCounter=0; // counts load peaks in direct seq
    public boolean pollQs() {
        CallEntry poll = pollQueues(cbQueues, queues); // first callback queues
        if (poll != null) {
            try {
                Object invoke = null;
                profileCounter++;
                if ( (profileCounter&511) == 0 && poll.getTarget() instanceof Actor) {
                    long nanos = System.nanoTime();
                    invoke = poll.getMethod().invoke(poll.getTarget(), poll.getArgs());
                    nanos = System.nanoTime()-nanos;
                    ((Actor)poll.getTarget()).__nanos += nanos;
                    if ( (profileCounter & (512*8-1)) == 0 ) {
                        profileCounter = 0;
//                        System.out.println("nanos "+poll.getTarget()+" "+((Actor)poll.getTarget()).__nanos);
                        int load = getLoad();
//                        System.out.println("LOAD " + load);
                        if ( load > 80 ) {
                            loadCounter++;
                            if ( loadCounter > 8  && instanceCount.get() < 8)
                            {
                                loadCounter = 0;
                                System.out.println("SPLIT");
                                DispatcherThread newOne = new DispatcherThread();
                                newOne.initNoStart(getQueueCapacity());
                                synchronized (queueList) {
                                    for (int i = 0; i < queueList.size(); i += 2) {
                                        Actor act = queueList.get(i);
                                        newOne.queueList.add(act);
                                        queueList.remove(act);
                                        i--;
                                    }
                                    applyQueueList();
                                    newOne.applyQueueList();
                                }
                                newOne.start();
                            }
                        }

                    }
                } else {
                    invoke = poll.getMethod().invoke(poll.getTarget(), poll.getArgs());
                }
                if (poll.getFutureCB() != null) {
                    final Future futureCB = poll.getFutureCB();   // the future of caller side
                    final Promise invokeResult = (Promise) invoke;  // the future returned sync from call
                    invokeResult.then(
                        new Callback() {
                               @Override
                               public void receiveResult(Object result, Object error) {
                                   futureCB.receiveResult(result, error );
                               }
                           }
                        );
                }
                return true;
            } catch (Exception e) {
                if (poll.getFutureCB() != null)
                    poll.getFutureCB().receiveResult(null, e);
                if (e.getCause() != null)
                    e.getCause().printStackTrace();
                else
                    e.printStackTrace();
            }
        }
        return false;
    }

    private int getLoad() {
        int res = 0;
        for (int i = 0; i < queues.length; i++) {
            MpscConcurrentQueue queue = (MpscConcurrentQueue) queues[i];
            int load = queue.size() * 100 / queue.getCapacity();
            if ( load > res )
                res = load;
        }
        return res;
    }

    public static void yield(int count) {
        backOffStrategy.yield(count);
    }

    // FIXME: bad for concurrentlinkedq
    public int getQSize() {
        int res = 0;
        for (int i = 0; i < queues.length; i++) {
            Queue queue = queues[i];
            res+=queue.size();
        }
        for (int i = 0; i < queues.length; i++) {
            Queue queue = cbQueues[i];
            res+=queue.size();
        }
        return res;
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

    public boolean isEmpty() {
        for (int i = 0; i < queues.length; i++) {
            Queue queue = queues[i];
            if ( ! queue.isEmpty() )
                return false;
        }
        for (int i = 0; i < cbQueues.length; i++) {
            Queue queue = cbQueues[i];
            if ( ! queue.isEmpty() )
                return false;
        }
        return true;
    }

    /**
     * blocking method, use for debugging only.
     */
    public void waitEmpty(long nanos) {
        while( ! isEmpty() )
            LockSupport.parkNanos(nanos);
    }

    public int getQueueCapacity() {
        return defQSize;
    }


    static class CallbackInvokeHandler implements InvocationHandler {

        final Object target;
        final Actor targetActor;

        public CallbackInvokeHandler(Object target, Actor act) {
            this.target = target;
            this.targetActor = act;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ( method.getDeclaringClass() == Object.class )
                return method.invoke(proxy,args); // toString, hashCode etc. invoke sync (danger if hashcode access local state)
            if ( target != null ) {
                CallEntry ce = new CallEntry(target,method,args, targetActor);
                Put2QueuePolling(targetActor.__cbQueue, ce);
            }
            return null;
        }
    }

    public static InvocationHandler getInvoker(Actor dispatcher, Object toWrap) {
        return new CallbackInvokeHandler(toWrap, dispatcher);
    }


}

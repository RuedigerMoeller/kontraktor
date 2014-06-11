package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.ArrayList;
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

    public static int DEFAULT_QUEUE_SIZE = 30000;

    ArrayList<Queue> queueList = new ArrayList<>();
    Queue queues[] = new Queue[0];
    Queue cbQueues[]= new Queue[0];
    int instanceNum;
    private int defaultQueueSize = DEFAULT_QUEUE_SIZE;
    volatile
    boolean shutDown = false;
    private boolean dead;
    private int defQSize;


    public int getDefaultQueueSize() {
        return defQSize;
    }

    class CallbackInvokeHandler implements InvocationHandler {

        final Object target;

        public CallbackInvokeHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ( method.getDeclaringClass() == Object.class )
                return method.invoke(proxy,args); // toString, hashCode etc.
            if ( target != null ) {
                CallEntry ce = new CallEntry(target,method,args,DispatcherThread.this);
                return dispatchCallback(ce);
            }
            return null;
        }
    }

    String stack; // contains stacktrace of creation of this

    private DispatcherThread(String dummy) {
    }

    public DispatcherThread() {
        init(DEFAULT_QUEUE_SIZE);
    }

    public DispatcherThread(int qSize) {
        init(qSize);
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

    public InvocationHandler getInvoker(Object toWrap) {
        return new CallbackInvokeHandler(toWrap);
    }

    protected void init(int qSize) {
        initNoStart(qSize);
        start();
    }

    protected void initNoStart(int qSize) {
        if (qSize<=0)
            qSize = DEFAULT_QUEUE_SIZE;
        defQSize = qSize;
        instanceNum = instanceCount.incrementAndGet();
        StringWriter stringWriter = new StringWriter(1000);
        PrintWriter s = new PrintWriter(stringWriter);
        new Exception().printStackTrace(s);
        s.flush();
        stack = stringWriter.getBuffer().toString();
        setName("ActorDisp spawned from ["+Thread.currentThread().getName()+"] "+System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return "DispatcherThread{" +
                " name:"+getName()+
                '}';
    }

    public void actorAdded(Actor a) {
        synchronized (queueList) {
            queueList.add(a.__mailbox);
        }
    }

    public void actorStopped(Actor a) {
        synchronized (queueList) {
            queueList.remove(a.__mailbox);
        }
    }

    public static Future pollDispatchOnObject(DispatcherThread currentThreadDispatcher, CallEntry e) {
        final Future fut;
        if (e.hasFutureResult()) {
            fut = new Promise();
            e.setFutureCB(new CallbackWrapper(currentThreadDispatcher,new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    fut.receiveResult(result,error);
                }
            }));
        } else
            fut = null;
        int count = 0;
        while ( e.getDispatcher().dispatchOnObject(e) ) {
            if ( currentThreadDispatcher != null ) {
                // FIXME: poll callback queue here
                currentThreadDispatcher.yield(count++);
            }
            else
                DispatcherThread.yield(count++);
        }
        return fut;
    }


    /**
     * @return true if blocked and polling channels should be done
     */
    public boolean dispatchOnObject( CallEntry entry ) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        if ( ! ((Actor)entry.getTarget()).__mailbox.offer(entry) ) {
            return true;
        }
        return false;
    }

    public boolean dispatchCallback( CallEntry ce ) {
        // MT sequential per actor ref
        if ( dead )
            throw new RuntimeException("received message on terminated dispatcher "+this);
        DispatcherThread sender = getThreadDispatcher();
        if ( ! cbQueue.offer(ce) ) {
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
    }

    int count = 0;
    protected CallEntry pollQueues() {
        if ( count >= queues.length ) {
            count = 0;
            if ( queues.length != queueList.size() ) {
                applyQueueList();
            }
            if ( queues.length == 0 ) {
                return null;
            }
        }
        CallEntry res = (CallEntry) queues[count].poll();
        count++;
        return res;
    }

    private void applyQueueList() {
        synchronized (queueList) {
            queues = new Queue[queueList.size()];
            for (int i = 0; i < queues.length; i++) {
                queues[i] = queueList.get(i);
            }
        }
    }


    // return true if msg was avaiable
    int profileCounter = 0;
    int loadCounter=0; // counts load peaks in direct seq
    public boolean pollQs() {
        CallEntry poll = (CallEntry) cbQueue.poll();
        if (poll == null)
            poll = pollQueues();
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
                                DispatcherThread newOne = new DispatcherThread("Dummy");
                                newOne.initNoStart(getDefaultQueueSize());
                                for (int i = 0; i < queues.length; i+=2) {
                                    Queue queue = queues[i];
                                    newOne.queueList.add(queue);
                                    queueList.remove(queue);
                                }
                                newOne.applyQueueList();
                                applyQueueList();
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

    public int getQSize() {
        int res = 0;
        for (int i = 0; i < queues.length; i++) {
            Queue queue = queues[i];
            res+=queue.size();
        }
        return res+cbQueue.size(); // FIXME: bad for concurrentlinkedq
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

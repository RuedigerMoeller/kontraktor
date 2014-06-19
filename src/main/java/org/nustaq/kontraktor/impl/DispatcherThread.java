package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
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
 * For more sophisticated applications it might be appropriate to manually set up dispatchers (Actors.assignDispatcher()).
 * The Actors.Channel method allows to specifiy a dedicated dispatcher on which to run the actor. This way it is possible
 * to exactly balance and control the number of threads created and which thread operates a set of actors.
 *
 */
public class DispatcherThread extends Thread {


    public static final int PROFILE_INTERVAL = 255;
    public static final int SCHEDULE_PER_PROFILE = 10;
    Scheduler scheduler;

    /**
     * defines the strategy applied when idling
     */
    ArrayList<Actor> queueList = new ArrayList<>();
    Queue queues[] = new Queue[0];
    Queue cbQueues[]= new Queue[0];

    protected int instanceNum;

    protected boolean shutDown = false;

    public DispatcherThread(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public String toString() {
        return "DispatcherThread{" +
                " name:"+getName()+
                '}';
    }

    public void addActor(Actor a) {
        synchronized (queueList) {
            queueList.add(a);
        }
    }

    public void removeActor(Actor a) {
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
                scheduler.yield(emptyCount);
                if (shutDown) // access volatile only when idle
                    isShutDown = true;
            }
        }
        scheduler.threadStopped(this);
    }

    // if list of queues to schedule has changed,
    // apply the change. needs to be done in thread
    public void applyQueueList() {
        synchronized (queueList) {
            if ( queueList.size() == 0 )
                shutDown = true;
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
    int schedCounter = 0;
    int loadCounter=0; // counts load peaks in direct seq
    int nextProfile = 511;
    long created = System.currentTimeMillis();

    public boolean pollQs() {
        CallEntry poll = pollQueues(cbQueues, queues); // first callback queues
        if (poll != null) {
            try {
                Actor.sender.set(poll.getTargetActor());
                Object invoke = null;
                profileCounter++;
                if (  profileCounter > nextProfile && queueList.size() > 1 && poll.getTarget() instanceof Actor ) {
                    profileCounter = 0;
                    invoke = profiledCall(poll);
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
            } catch ( Exception e) {
                if ( e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() == ActorStoppedException.Instance ) {
                    removeActor((Actor) poll.getTarget());
                    applyQueueList();
                    return true;
                }
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

    private Object profiledCall(CallEntry poll) throws IllegalAccessException, InvocationTargetException {
        nextProfile = (int) (PROFILE_INTERVAL + Math.random() * 13);
        schedCounter++;

        long nanos = System.nanoTime();
        Object invoke = poll.getMethod().invoke(poll.getTarget(), poll.getArgs());
        nanos = System.nanoTime() - nanos;
        ((Actor) poll.getTarget()).__nanos = (((Actor) poll.getTarget()).__nanos * 7 + nanos) / 8;

        if (schedCounter > SCHEDULE_PER_PROFILE) {
            schedCounter = 0;
            checkForSplit();
        }
        return invoke;
    }

    private void checkForSplit() {
        int load = getLoad();
        if (load > 80 && queueList.size() > 1 && System.currentTimeMillis()-created > 100 ) {
            loadCounter++;
            if (loadCounter > 2) {
                loadCounter = 0;
                doSplit();
            }
        }
    }

    private void doSplit() {
        scheduler.rebalance(this);
    }

    // must be called in thread
    public void splitTo( DispatcherThread newOne ) {
        System.out.println("SPLIT " + scheduler.getMaxThreads());
        synchronized (queueList) {
            long myTime = 0;
            long otherTime = 0;
            Collections.sort(queueList, new Comparator<Actor>() {
                @Override
                public int compare(Actor o1, Actor o2) {
                    return (o1.__nanos - o2.__nanos) > 0 ? -1 : 1;
                }
            });
            for (int i = 0; i < queueList.size(); i++) {
                Actor act = queueList.get(i);
                long nan = act.__nanos;
                if (otherTime < myTime) {
                    otherTime += nan;
                } else {
                    myTime += nan;
                }
            }
            //if ( 8*myTime > otherTime && 8*otherTime > myTime )
            //                                    {
            myTime = otherTime = 0;
            for (int i = 0; i < queueList.size(); i++) {
                Actor act = queueList.get(i);
                long nan = act.__nanos;
                if (otherTime < myTime) {
                    newOne.queueList.add(act);
                    queueList.remove(act);
                    i--;
                    otherTime += nan;
                    act.__currentDispatcher = newOne;
                } else {
                    myTime += nan;
                }
            }
            System.out.println("distributeion " + myTime + ":" + otherTime + " actors " + queues.length);
            created = System.currentTimeMillis();
            applyQueueList();
            newOne.applyQueueList();
        }
    }

    public int getLoad() {
        int res = 0;
        for (int i = 0; i < queues.length; i++) {
            MpscConcurrentQueue queue = (MpscConcurrentQueue) queues[i];
            int load = queue.size() * 100 / queue.getCapacity();
            if ( load > res )
                res = load;
        }
        return res;
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

    public Scheduler getScheduler() {
        return scheduler;
    }
}

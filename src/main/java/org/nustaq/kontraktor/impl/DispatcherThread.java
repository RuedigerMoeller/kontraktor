package org.nustaq.kontraktor.impl;

import io.jaq.mpsc.MpscConcurrentQueue;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.RemoteRegistry;
import org.nustaq.kontraktor.util.Log;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * For more sophisticated applications it might be appropriate to manually set up dispatchers (Actors.assignDispatcher()).
 * The Actors.Channel method allows to specifiy a dedicated dispatcher on which to run the actor. This way it is possible
 * to exactly balance and control the number of threads created and which thread operates a set of actors.
 *
 */
public class DispatcherThread extends Thread implements Monitorable {

    public static boolean DUMP_CATCHED = false; // do a print stacktrace on uncatched exceptions put as a future's result
    public static int SCHEDULE_TICK_NANOS = 1000*500; // how often balancing/profiling is done
    public static int QUEUE_PERCENTAGE_TRIGGERING_REBALANCE = 50;      // if queue is X % full, consider rebalance
    public static int MILLIS_AFTER_CREATION_BEFORE_REBALANCING = 2; // give caches a chance to get things going before rebalancing

    private Scheduler scheduler;

    private Actor actors[] = new Actor[0]; // always refs
    ConcurrentLinkedQueue<Actor> toAdd = new ConcurrentLinkedQueue<>();

    protected boolean shutDown = false;
    static AtomicInteger dtcount = new AtomicInteger(0);



    public ArrayList __stack = new ArrayList();
    volatile boolean isIsolated = false;
    private boolean autoShutDown = true;

    public DispatcherThread(Scheduler scheduler) {
        this.scheduler = scheduler;
        setName("DispatcherThread "+dtcount.incrementAndGet());
    }

    public DispatcherThread(Scheduler scheduler, boolean autoShutDown) {
        this.autoShutDown = autoShutDown;
        this.scheduler = scheduler;
        setName("DispatcherThread "+dtcount.incrementAndGet());
    }

    @Override
    public String toString() {
        return "DispatcherThread{" +
                " name:"+getName()+
                '}';
    }

    public boolean isIsolated() {
        return isIsolated;
    }

    public void setIsolated(boolean isIsolated) {
        this.isIsolated = isIsolated;
    }

    public void addActor(Actor act) {
        act.getActorRef().__currentDispatcher = act.getActor().__currentDispatcher = this;
        toAdd.offer(act.getActorRef());
    }

    // removes immediate must be called from this thread
    void removeActorImmediate(Actor act) {
        if ( Thread.currentThread() != this )
            throw new RuntimeException("wrong thread");
        Actor newAct[] = new Actor[actors.length-1];
        int idx = 0;
        for (int i = 0; i < actors.length; i++) {
            Actor actor = actors[i];
            if ( actor != act)
                newAct[idx++] = actor;
        }
        if ( idx != newAct.length )
            throw new RuntimeException("could not remove actor");
        actors = newAct;
    }

    int emptySinceLastCheck = 0; // incremented on sleep/all

    public void run() {
        int emptyCount = 0;
        long scheduleTickTime = System.nanoTime();
        boolean isShutDown = false;
        while( ! isShutDown ) {
            try {
                if ( pollQs() ) {
                    emptyCount = 0;
                    if ( System.nanoTime() - scheduleTickTime > SCHEDULE_TICK_NANOS) {
                        if ( emptySinceLastCheck == 0 ) // no idle during last interval
                        {
                            checkForSplit();
                        }
                        emptySinceLastCheck = 0;
                        scheduleTickTime = System.nanoTime();
                        schedulePendingAdds();
                    }
                }
                else {
                    emptyCount++;
                    emptySinceLastCheck++;
                    scheduler.pollDelay(emptyCount);
                    if (shutDown) // access volatile only when idle
                        isShutDown = true;
                    if ( scheduler.getBackoffStrategy().isSleeping(emptyCount) ) {
                        scheduleTickTime = 0;
                        schedulePendingAdds();
                        if ( System.currentTimeMillis()-created > 1000 ) {
                            if ( autoShutDown && actors.length == 0 && toAdd.peek() == null) {
                                shutDown();
                            } else {
                                scheduler.tryStopThread(this);
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                Log.Warn(this, th, "from main poll loop");
            }
        }
        scheduler.threadStopped(this);
        for ( int i = 0; i < 100; i++ ) { // FIXME: umh .. works in practice
            LockSupport.parkNanos(1000*1000*5);
            while ( actors.length > 0 || toAdd.peek() != null ) {
                if ( SimpleScheduler.DEBUG_SCHEDULING)
                    Log.Lg.warn(this, "Severe: zombie dispatcher thread detected");
                scheduler.tryStopThread(this);
                i = 0;
            }
        }
        if ( SimpleScheduler.DEBUG_SCHEDULING)
            Log.Info(this,"dispatcher thread terminated "+getName());
    }

    /**
     * add actors which have been marked to be scheduled on this
     */
    public void schedulePendingAdds() {
        ArrayList<Actor> newOnes = new ArrayList<>();
        Actor a;
        while ( (a=toAdd.poll()) != null ) {
            newOnes.add(a);
        }
        if ( newOnes.size() > 0 ) {
            Actor newQueue[] = new Actor[newOnes.size()+actors.length];
            System.arraycopy(actors,0,newQueue,0,actors.length);
            for (int i = 0; i < newOnes.size(); i++) {
                Actor actor = newOnes.get(i);
                newQueue[actors.length+i] = actor;
            }
            actors = newQueue;
        }

    }

    // poll all actors in queue arr round robin
    int count = 0;
    protected CallEntry pollQueues(Actor[] actors) {
        if ( count >= actors.length ) {
            // check for changed queueList each run FIXME: too often !
            count = 0;
            if ( actors.length == 0 ) {
                return null;
            }
        }
        Actor actor2poll = actors[count];
        CallEntry res = (CallEntry) actor2poll.__cbQueue.poll();
        if ( res == null )
            res = (CallEntry) actor2poll.__mailbox.poll();
        count++;
        return res;
    }


    // return true if msg was avaiable
    long created = System.currentTimeMillis();

    /**
     * @return false if no message could be polled
     */
    public boolean pollQs() {
        return pollQs(actors);
    }

    /**
     * @return false if no message could be polled
     */
    public boolean pollQs(Actor actors[]) {
        CallEntry callEntry = pollQueues(actors);
        if (callEntry != null) {
            try {
                // before calling the actor method, set current sender
                // to target, so for each method/callback invoked by the actor method,
                // sender has correct value
                Actor targetActor = callEntry.getTargetActor();
                Actor.sender.set(targetActor);
                if (targetActor.__stopped) {
                    targetActor.__addDeadLetter(targetActor,callEntry.getMethod().getName());
                    return true;
                }
                Object invoke = null;
                try {
                    invoke = invoke(callEntry);
                } catch (IllegalArgumentException iae) {
                    // FIXME: boolean is translated wrong by minbin .. this fix is expensive
                    final Class<?>[] parameterTypes = callEntry.getMethod().getParameterTypes();
                    final Object[] args = callEntry.getArgs();
                    if ( args.length == parameterTypes.length ) {
                        for (int i = 0; i < args.length; i++) {
                            Object arg = args[i];
                            if ( (parameterTypes[i] == boolean.class || parameterTypes[i] == Boolean.class) &&
                                 arg instanceof Byte ) {
                                args[i] = ((Byte) arg).intValue()!=0;
                            }
                        }
                        invoke = invoke(callEntry);
                    } else {
                        System.out.println("mismatch when invoking method " + callEntry);
                        for (int i = 0; i < callEntry.getArgs().length; i++) {
                            Object o = callEntry.getArgs()[i];
                            System.out.println("arg " + i + " " + o + (o != null ? " " + o.getClass().getSimpleName() : "") + ",");
                        }
                        System.out.println();
                        throw iae;
                    }
                }
                if (callEntry.getFutureCB() != null) {
                    final IPromise futureCB = callEntry.getFutureCB();   // the future of caller side
                    final Promise invokeResult = (Promise) invoke;  // the future returned sync from call
                    if ( invokeResult != null ) { // if return null instead a promise, method is handled like void
                        invokeResult.then(
                            new Callback() {
                                @Override
                                public void complete(Object result, Object error) {
                                    futureCB.complete(result, error);
                                }
                            }
                        );
                    }
                }
                return true;
            } catch ( Throwable e) {
                if ( e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() == InternalActorStoppedException.Instance ) {
                    // fixme: rare classcast exception with elasticscheduler seen here when $stop is called from a callback ..
                    Actor actor = (Actor) callEntry.getTarget();
                    actor.__stopped = true;
                    removeActorImmediate(actor.getActorRef());
// FIXME: Many Testcases fail if uncommented. Rethink
//                    if (callEntry.getFutureCB() != null)
//                        callEntry.getFutureCB().complete(null, e);
//                    else
//                        Log.Warn(this,e,"");
//                    if (callEntry.getFutureCB() != null)
//                        callEntry.getFutureCB().complete(null, e);
//                    else
//                        Log.Warn(this,e,"");
                    return true;
                }
                if ( e instanceof InvocationTargetException ) {
                    e = e.getCause();
                }
                if (callEntry.getFutureCB() != null) {
                    Log.Warn(this, e, "unhandled exception in message: '"+callEntry+"'.returned catched exception to future " + e + " set DispatcherThread.DUMP_CATCHED to true in order to dump stack.");
                    if ( DUMP_CATCHED ) {
                        e.printStackTrace();
                    }
                    callEntry.getFutureCB().complete(null, e);
                }
                else
                    Log.Warn(this,e,"");
            }
        }
        return false;
    }

    private Object invoke(CallEntry poll) throws IllegalAccessException, InvocationTargetException {
        final Object target = poll.getTarget();
        RemoteRegistry remoteRefRegistry = poll.getRemoteRefRegistry();
        Actor.registry.set(remoteRefRegistry);
        return poll.getMethod().invoke(target, poll.getArgs());
    }

    private void checkForSplit() {
        int load = getLoad();
        if (load > QUEUE_PERCENTAGE_TRIGGERING_REBALANCE &&
            actors.length > 1 &&
            System.currentTimeMillis()-created > MILLIS_AFTER_CREATION_BEFORE_REBALANCING )
        {
            scheduler.rebalance(this);
        }
    }


    /**
     * @return percentage of queue fill of max actor
     */
    public int getLoad() {
        int res = 0;
        final Actor actors[] = this.actors;
        for (int i = 0; i < actors.length; i++) {
            MpscConcurrentQueue queue = (MpscConcurrentQueue) actors[i].__mailbox;
            int load = queue.size() * 100 / queue.getCapacity();
            if ( load > res )
                res = load;
            queue = (MpscConcurrentQueue) actors[i].__cbQueue;
            load = queue.size() * 100 / queue.getCapacity();
            if ( load > res )
                res = load;
        }
        return res;
    }

    /**
     * accumulated queue sizes of all actors
     * @return
     */
    public int getAccumulatedQSizes() {
        int res = 0;
        final Actor actors[] = this.actors;
        for (int i = 0; i < actors.length; i++) {
            res += actors[i].getQSizes();
        }
        return res;
    }

    /**
     * @return accumulated q size of all dispatched actors
     */
    public int getQSize() {
        int res = 0;
        final Actor actors[] = this.actors;
        for (int i = 0; i < actors.length; i++) {
            Actor a = actors[i];
            res+=a.__mailbox.size();
            res+=a.__cbQueue.size();
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
        for (int i = 0; i < actors.length; i++) {
            Actor act = actors[i];
            if ( ! act.__mailbox.isEmpty() || ! act.__cbQueue.isEmpty() )
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

    /**
     * @return a copy of actors used
     */
    public Actor[] getActors() {
        Actor actors[] = this.actors;
        Actor res[] = new Actor[actors.length];
        System.arraycopy(actors,0,res,0,res.length);
        return res;
    }

    Actor[] getActorsNoCopy() {
        return actors;
    }

    /**
     * can be called from the dispacther thread itself only
     * @param receiverRef
     * @return
     */
    public boolean schedules(Object receiverRef) {
        if ( Thread.currentThread() != this ) {
            throw new RuntimeException("cannot call from foreign thread");
        }
        if ( receiverRef instanceof Actor ) {
            // FIXME: think about visibility of scheduler var
            return ((Actor) receiverRef).__currentDispatcher == this;
//            for (int i = 0; i < actors.length; i++) {
//                Actor actor = actors[i];
//                if (actor == receiverRef)
//                    return true;
//            }
        }
        return false;
    }

    @Override
    public IPromise $getReport() {
        return new Promise(new DispatcherReport(getName(), actors.length, getLoad(),getAccumulatedQSizes() ));
    }

    @Override
    public IPromise<Monitorable[]> $getSubMonitorables() {
        return new Promise(getActors());
    }

    public static class DispatcherReport {

        String name;
        int numActors;
        int loadPerc;
        int qSizes;

        public DispatcherReport() {
        }

        public DispatcherReport(String name, int numActors, int loadPerc, int qSizes) {
            this.name = name;
            this.numActors = numActors;
            this.loadPerc = loadPerc;
            this.qSizes = qSizes;
        }

        public String getName() {
            return name;
        }

        public int getNumActors() {
            return numActors;
        }
    }

}

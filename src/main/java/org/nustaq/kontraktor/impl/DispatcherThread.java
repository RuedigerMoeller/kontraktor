package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    public static final int SCHEDULE_PER_PROFILE = 32;
    private Scheduler scheduler;

    /**
     * defines the strategy applied when idling
     */
    private Actor actors[] = new Actor[0];
    ConcurrentLinkedQueue<Actor> toAdd = new ConcurrentLinkedQueue<>();

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

    public void addActor(Actor act) {
        toAdd.offer(act);
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

    public void run() {
        int emptyCount = 0;
        int scheduleNewActorCount = 0;
        boolean isShutDown = false;
        while( ! isShutDown ) {
            if ( pollQs() ) {
                emptyCount = 0;
                scheduleNewActorCount++;
                if ( scheduleNewActorCount > 500 ) {
                    scheduleNewActorCount = 0;
                    schedulePendingAdds();
                }
            }
            else {
                emptyCount++;
                scheduler.yield(emptyCount);
                if (shutDown) // access volatile only when idle
                    isShutDown = true;
                if ( scheduler.getBackoffStrategy().isSleeping(emptyCount) ) {
                    scheduleNewActorCount = 0;
                    schedulePendingAdds();
                    if ( System.currentTimeMillis()-created > 3000 ) {
                        if ( actors.length == 0 && toAdd.peek() == null ) {
                            shutDown();
                        } else {
                            scheduler.tryStopThread(this);
                        }
                    }
                }
            }
        }
        scheduler.threadStopped(this);
        for ( int i = 0; i < 100; i++ ) {
            LockSupport.parkNanos(1000*1000*5);
            if ( actors.length > 0 ) {
                System.out.println("Severe: zombie dispatcher thread detected");
                scheduler.tryStopThread(this);
                i = 0;
            }
        }
        System.out.println("thread died");
    }

    private void schedulePendingAdds() {
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
        CallEntry res = (CallEntry) actors[count].__cbQueue.poll();
        if ( res == null )
            res = (CallEntry) actors[count].__mailbox.poll();
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
        CallEntry poll = pollQueues(actors); // first callback actors
        if (poll != null) {
            try {
                Actor.sender.set(poll.getTargetActor());
                Object invoke = null;
                profileCounter++;
                if (  profileCounter > nextProfile && poll.getTarget() instanceof Actor ) {
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
                    Actor actor = (Actor) poll.getTarget();
                    actor.__stopped = true;
                    removeActorImmediate(actor);
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
        ((Actor) poll.getTarget()).__nanos = (((Actor) poll.getTarget()).__nanos * 31 + nanos) / 32;

        if (schedCounter > SCHEDULE_PER_PROFILE) {
            schedCounter = 0;
            checkForSplit();
        }
        return invoke;
    }

    private void checkForSplit() {
        int load = getLoad();
        if (load > 80 && actors.length > 1 && System.currentTimeMillis()-created > 1000 ) { // FIXME: constant
            loadCounter++;
            if (loadCounter > 2) { // FIXME: constant
                loadCounter = 0;
                scheduler.rebalance(this);
            }
        }
    }

    // must be called in thread. newOne is expected to not yet started
    void splitTo( DispatcherThread newOne ) {
        System.out.println("SPLIT " + scheduler.getMaxThreads());
        long myTime = 0;
        long otherTime = 0;
        Arrays.sort(actors, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return (((Actor)o1).__nanos - ((Actor)o2).__nanos) > 0 ? -1 : 1;
            }
        });
        for (int i = 0; i < actors.length; i++) {
            Actor act = (Actor) actors[i];
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
        ArrayList<Actor> new2ScheduleOnMe = new ArrayList<>();
        ArrayList<Actor> new2ScheduleOnOther = new ArrayList<>();
        for (int i = 0; i < actors.length; i++) {
            Actor act = (Actor) actors[i];
            long nan = act.__nanos;
            if (otherTime < myTime) {
                new2ScheduleOnOther.add(act);
                otherTime += nan;
                act.__currentDispatcher = newOne;
            } else {
                new2ScheduleOnMe.add(act);
                myTime += nan;
            }
        }
        actors = new Actor[new2ScheduleOnMe.size()];
        new2ScheduleOnMe.toArray(actors);
        newOne.actors = new Actor[new2ScheduleOnOther.size()];
        new2ScheduleOnOther.toArray(newOne.actors);
        System.out.println("distributeion " + myTime + ":" + otherTime + " actors " + actors.length);
        created = System.currentTimeMillis();
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
        }
        return res;
    }

    /**
     * @return profiling based load
     */
    public long getLoadNanos() {
        long res = 0;
        final Actor actors[] = this.actors;
        for (int i = 0; i < actors.length; i++) {
            Actor a = actors[i];
            res += a.__nanos;
        }
        return res;
    }

    // FIXME: bad for concurrentlinkedq
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

    public Actor[] getActors() {
        Actor actors[] = this.actors;
        Actor res[] = new Actor[actors.length];
        System.arraycopy(actors,0,res,0,res.length);
        return res;
    }
}

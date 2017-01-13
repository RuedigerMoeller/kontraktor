/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.impl;

import org.eclipse.jetty.util.MyConcurrentArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.serialization.util.FSTUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ruedi on 25/03/15.
 */
public class ActorsImpl {

    protected ConcurrentLinkedQueue deadLetters = new ConcurrentLinkedQueue();
    protected ActorProxyFactory factory;

    public ActorsImpl() {
        factory = new ActorProxyFactory();
    }


    public ActorProxyFactory getFactory() {
        return factory;
    }

    public ConcurrentLinkedQueue getDeadLetters() {
        return deadLetters;
    }

    public Actor makeProxy(Class<? extends Actor> clz, DispatcherThread disp, int qs) {
        try {
            if ( qs <= 100 )
                qs = disp.getScheduler().getDefaultQSize();

            qs = FSTUtil.nextPow2(qs);

            Actor realActor = clz.newInstance();
            realActor.__mailbox =  createQueue(qs);
            realActor.__mailboxCapacity = qs;
            realActor.__mbCapacity = realActor.__mailboxCapacity; // wtf
            realActor.__cbQueue =  createQueue(qs);

            Actor selfproxy = getFactory().instantiateProxy(realActor);
            realActor.__self = selfproxy;
            selfproxy.__self = selfproxy;

            selfproxy.__mailbox = realActor.__mailbox;
            selfproxy.__mailboxCapacity = qs;
            selfproxy.__mbCapacity = realActor.__mbCapacity;
            selfproxy.__cbQueue = realActor.__cbQueue;

            realActor.__scheduler = disp.getScheduler();
            selfproxy.__scheduler = disp.getScheduler();

            realActor.__currentDispatcher = disp;
            selfproxy.__currentDispatcher = disp;

            disp.addActor(realActor);
            return selfproxy;
        } catch (Exception e) {
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    public Queue createQueue(int qSize) {
        if ( Actors.DEFAULT_BOUNDED_QUEUE )
            return new MpscArrayQueue<>(qSize);
        else
            return new MyConcurrentArrayQueue<>( Math.max(512, qSize/10) );
    }

    public Actor newProxy(Class<? extends Actor> clz, Scheduler sched, int qsize) {
        if ( sched == null ) {
            if (Thread.currentThread() instanceof DispatcherThread) {
                sched = ((DispatcherThread) Thread.currentThread()).getScheduler();
            }
        }
        try {
            if ( sched == null )
                sched = Actors.defaultScheduler.get();
            if ( qsize < 1 )
                qsize = sched.getDefaultQSize();
            return makeProxy(clz, sched.assignDispatcher(70), qsize);
        } catch (Exception e) {
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

}

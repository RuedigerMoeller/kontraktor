package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.impl.*;
import io.jaq.mpsc.MpscConcurrentQueue;

import java.util.Queue;

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
 * Date: 04.01.14
 * Time: 19:50
 * To change this template use File | Settings | File Templates.
 */
public class Actors {

    static Actors instance = new Actors();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz, -1);
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, int qSize) {
        return (T) instance.newProxy(actorClazz, qSize);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, DispatcherThread disp) {
        return (T) instance.newProxy(actorClazz,disp);
    }

    /**
     * create a new actor with a newly created DispatcherThread
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T SpawnActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz, instance.newDispatcher(-1) );
    }

    public static <T extends Actor> T $$( Class<T> clz ) {
        try {
            T seqproxy = instance.getFactory().instantiateProxy(clz.newInstance());
            seqproxy.__isSeq = true;
            return seqproxy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * create a new actor with a newly created DispatcherThread
     * @param actorClazz
     * @param <T>
     * @param qSiz - size of mailbox queue
     * @return
     */
    public static <T extends Actor> T SpawnActor(Class<? extends Actor> actorClazz, int qSiz) {
        return (T) instance.newProxy(actorClazz, instance.newDispatcher(qSiz) );
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Scheduler scheduler = new ElasticScheduler(8,10000);

    protected Actors() {
        factory = new ActorProxyFactory();
    }

    protected ActorProxyFactory factory;

    protected ActorProxyFactory getFactory() {
        return factory;
    }

    protected Actor newProxy(Class<? extends Actor> clz, DispatcherThread disp ) {
        try {
            int qs = disp.getQSize();
            if ( disp.getQSize() <= 0 )
                qs = disp.getQueueCapacity();

            Actor realActor = clz.newInstance();
            realActor.__mailbox =  createQueue(qs);
            realActor.__cbQueue =  createQueue(qs);

            Actor selfproxy = getFactory().instantiateProxy(realActor);
            realActor.__self = selfproxy;
            selfproxy.__mailbox = realActor.__mailbox;
            selfproxy.__cbQueue = realActor.__cbQueue;

            Actor seqproxy = getFactory().instantiateProxy(realActor);
            seqproxy.__isSeq = true;

            realActor.__seq = seqproxy;
            selfproxy.__seq = seqproxy;
            realActor.__scheduler = scheduler;
            selfproxy.__scheduler = scheduler;

            realActor.__currentDispatcher = disp;
            selfproxy.__currentDispatcher = disp;

            disp.actorAdded(realActor);
            return selfproxy;
        } catch (Exception e) {
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    protected Queue createQueue(int qSize) {
        return new MpscConcurrentQueue(qSize);
    }

    protected Actor newProxy(Class<? extends Actor> clz, int qsize) {
        if ( Thread.currentThread() instanceof DispatcherThread ) {
            return newProxy( clz, (DispatcherThread) Thread.currentThread());
        } else
        {
            try {
                return newProxy(clz, newDispatcher(qsize));
            } catch (Exception e) {
                if ( e instanceof RuntimeException)
                    throw (RuntimeException)e;
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * return a new dispatcher backed by a new thread. Overriding classes should *not*
     * return existing dispatchers here, as this can be used to isolate blocking code from the actor flow.
     *
     * if qSiz lesser or equal 0 use default size
     * @return
     */
    protected DispatcherThread newDispatcher(int qSize) {
        DispatcherThread dispatcherThread = new DispatcherThread(scheduler, qSize);
        dispatcherThread.start();
        return dispatcherThread;
    }

}

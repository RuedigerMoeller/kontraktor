package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.*;
import io.jaq.mpsc.MpscConcurrentQueue;
import org.nustaq.kontraktor.util.Log;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    public static Actors instance = new Actors(); // public for testing

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    public static void AddDeadLetter(String s) {
        Log.Lg.warn(null,s);
        DeadLetters().add(s);
    }

    /**
     * messages that have been dropped or have been sent to stopped actors
     *
     * @return queue of dead letters. Note: only strings are recorded to avoid accidental references.
     */
    public static ConcurrentLinkedQueue<String> DeadLetters() {
        return instance.deadLetters;
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz, new ElasticScheduler(1), -1);
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
        return (T) instance.newProxy(actorClazz, new ElasticScheduler(1), qSize);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, Scheduler scheduler) {
        return (T) instance.newProxy(actorClazz,scheduler,-1);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, Scheduler scheduler, int qsize) {
        return (T) instance.newProxy(actorClazz,scheduler,qsize);
    }

    public static Future<Future[]> yield(Future... futures) {
        Promise res = new Promise();
        yield(futures, 0, res);
        return res;
    }

    public static <T> Future<List<Future<T>>> yield(List<Future<T>> futures) {
        Promise res = new Promise();
        yield(futures, 0, res);
        return res;
    }

    /**
     * mimics scala's async macro. callables are executed in order. if a callable returns a promise,
     * execution of the next callable is deferred until the promise is fulfilled. The future resulting from the last
     * callable is returned.
     * Helps keeping future chains readable.
     *
     * (see https://gist.github.com/RuedigerMoeller/10c583819616f2563969 for an example)
     *
     * @param toexec
     * @return
     */
    public static Future<Future[]> async(Callable<Future>... toexec) {
        return ordered(toexec,0);
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static Future ordered(final Callable<Future> callables[], final int index) {
        try {
            if ( index == callables.length - 1 ) {
                return callables[index].call();
            } else {
                Future res = callables[index].call();
                if ( res != null ) {
                    Promise p = new Promise();
                    res.then( () -> ordered(callables, index +1).then(p) );
                    return p;
                } else
                    return ordered(callables, index +1 );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise<>(null,e);
        }
    }

    private static void yield(final Future futures[], final int index, final Future result) {
        if ( index < futures.length ) {
            futures[index].then( (r,e) -> yield(futures, index + 1, result) );
        } else {
            result.receive(futures, null);
        }
    }

    private static <T> void yield(final List<Future<T>> futures, final int index, final Future result) {
        if ( index < futures.size() ) {
            futures.get(index).then( (r,e) -> yield(futures, index + 1, result) );
        } else {
            result.receive(futures, null);
        }
    }

    //// instance

    ConcurrentLinkedQueue deadLetters = new ConcurrentLinkedQueue();

    protected Actors() {
        factory = new ActorProxyFactory();
    }

    protected ActorProxyFactory factory;

    public ActorProxyFactory getFactory() {
        return factory;
    }

    protected Actor makeProxy(Class<? extends Actor> clz, DispatcherThread disp, int qs) {
        try {
            if ( qs <= 100 )
                qs = disp.getScheduler().getDefaultQSize();

            Actor realActor = clz.newInstance();
            realActor.__mailbox =  createQueue(qs);
            realActor.__mbCapacity = qs;
            realActor.__cbQueue =  createQueue(qs);

            Actor selfproxy = getFactory().instantiateProxy(realActor);
            realActor.__self = selfproxy;
            selfproxy.__self = selfproxy;

            selfproxy.__mailbox = realActor.__mailbox;
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

    protected Queue createQueue(int qSize) {
        return new MpscConcurrentQueue(qSize);
    }

    protected Actor newProxy(Class<? extends Actor> clz, Scheduler sched, int qsize) {
        if ( sched == null ) {
            if (Thread.currentThread() instanceof DispatcherThread) {
                sched = ((DispatcherThread) Thread.currentThread()).getScheduler();
            }
        }
        try {
            if ( sched == null )
                sched = new ElasticScheduler(1,qsize);
            if ( qsize < 1 )
                qsize = sched.getDefaultQSize();
            return makeProxy(clz, sched.assignDispatcher(70), qsize);
        } catch (Exception e) {
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    public static Object[] toResults(Future[] futs) {
        Object o[] = new Object[futs.length];
        for (int i = 0; i < o.length; i++) {
            o[i] = futs [i].getResult();
        }
        return o;
    }
}

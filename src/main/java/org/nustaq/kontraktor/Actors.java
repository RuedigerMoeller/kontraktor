package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.util.Log;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
 *
 * A set of static async helper methods. Note Actor inherits from this class.
 *
 */
public class Actors {

    public static final int MAX_EXTERNAL_THREADS_POOL_SIZE = 1000; // max threads used when externalizing blocking api
    public static ExecutorService exec = Executors.newFixedThreadPool(MAX_EXTERNAL_THREADS_POOL_SIZE);
    public static ActorsImpl instance = new ActorsImpl(); // public for testing
    public static Timer delayedCalls = new Timer();

    public static Supplier<Scheduler> defaultScheduler = () -> new SimpleScheduler();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    /**
     * utility function. Executed in foreign thread. Use Actor::delayed() to have the runnable executed inside actor thread
     */
    public static void SubmitDelayed( long millis, Runnable task ) {
        Actors.delayedCalls.schedule( new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        },millis);
    }

    public static void SubmitPeriodic( long startMillis, Function<Long,Long> task ) {
        Actors.delayedCalls.schedule( new TimerTask() {
            @Override
            public void run() {
                Long tim = task.apply(startMillis);
                if ( tim != null && tim > 0 ) {
                    SubmitPeriodic(tim, in -> task.apply(in) );
                }
            }
        },startMillis);
    }


    public static void AddDeadLetter(String s) {
        Log.Lg.warn(null,s);
        DeadLetters().add(s);
    }

    /**
     * in case called from an actor, wraps the given interface instance into a proxy such that
     * a calls on the interface get schedulled on the actors thread (avoids accidental multithreading
     * when handing out callback/listener interfaces from an actor)
     * @param anInterface
     * @param <T>
     * @return
     */
    public static <T> T InThread( T anInterface ) {
        Actor sender = Actor.sender.get();
        if ( sender != null )
            return sender.getScheduler().inThread(sender.getActor(),anInterface);
        else
            return anInterface;
    }

    /**
     * messages that have been dropped or have been sent to stopped actors
     *
     * @return queue of dead letters. Note: only strings are recorded to avoid accidental references.
     */
    public static ConcurrentLinkedQueue<String> DeadLetters() {
        return instance.getDeadLetters();
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<T> actorClazz) {
        return (T) instance.newProxy(actorClazz, defaultScheduler.get(), -1);
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<T> actorClazz, int qSize) {
        return (T) instance.newProxy(actorClazz, defaultScheduler.get(), qSize);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<T> actorClazz, Scheduler scheduler) {
        return (T) instance.newProxy(actorClazz,scheduler,-1);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<T> actorClazz, Scheduler scheduler, int qsize) {
        return (T) instance.newProxy(actorClazz,scheduler,qsize);
    }

    /**
     * similar to es6 Promise.all method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once all promises provided are settled
     *
     */
    public static <T> IPromise<IPromise<T>[]> all(IPromise<T>... futures) {
        Promise res = new Promise();
        awaitSettle(futures, 0, res);
        return res;
    }

    /**
     * similar to es6 Promise.all method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once all promises provided are settled
     *
     */
    public static <T> IPromise<List<IPromise<T>>> all(List<IPromise<T>> futures) {
        Promise res = new Promise();
        awaitSettle(futures, 0, res);
        return res;
    }

    /**
     * await until all futures are settled and stream them
     *
     */
    protected <T> Stream<T> stream(IPromise<T>... futures) {
        return streamHelper(all(futures).await());
    }

    protected <T> Stream<T> stream(List<IPromise<T>> futures) {
        return streamHelper(all(futures).await());
    }

    /**
     * similar to es6 Promise.race method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once one of the futures provided gets settled
     *
     */
    public static <T> IPromise<T> race( IPromise<T>... futures ) {
        Promise p = new Promise();
        AtomicBoolean fin = new AtomicBoolean(false);
        for (int i = 0; i < futures.length; i++) {
            futures[i].then( (r,e) -> {
                if ( fin.compareAndSet(false,true) ) {
                    p.complete(r, e);
                }
            });
        }
        return p;
    }

    /**
     * similar to es6 Promise.race method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once one of the futures provided gets settled
     *
     */
    public static <T> IPromise<T> race( Collection<IPromise<T>> futures ) {
        Promise p = new Promise();
        AtomicBoolean fin = new AtomicBoolean(false);
        for (Iterator<IPromise<T>> iterator = futures.iterator(); iterator.hasNext(); ) {
            iterator.next().then( (r,e) -> {
                if ( fin.compareAndSet(false,true) ) {
                    p.complete(r, e);
                }
            });
        }
        return p;
    }

    /**
     * plain utility additiona to java 8 streams
     *
     * @param t
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream( T ... t ) {
        return Arrays.stream(t);
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // helper

    private static <T> void awaitSettle(final IPromise<T> futures[], final int index, final IPromise result) {
        if ( index < futures.length ) {
            futures[index].then( (r,e) -> awaitSettle(futures, index + 1, result) );
        } else {
            result.complete(futures, null);
        }
    }

    private static <T> void awaitSettle(final List<IPromise<T>> futures, final int index, final IPromise result) {
        if ( index < futures.size() ) {
            futures.get(index).then((r, e) -> awaitSettle(futures, index + 1, result));
        } else {
            result.complete(futures, null);
        }
    }

    /**
     * helper to stream settled futures unboxed. e.g. all(f1,f2,..).then( farr -> stream(farr).forEach( val -> process(val) );
     * Note this can be used only on "settled" or "completed" futures. If one of the futures has been rejected,
     * a null value is streamed.
     *
     * @param completedPromises
     * @param <T>
     * @return
     */
    private static <T> Stream<T> streamHelper(IPromise<T>... completedPromises) {
        return Arrays.stream(completedPromises).map(future -> future.get());
    }

    private static <T> Stream<T> streamHelper(List<IPromise<T>> completedPromises) {
        return completedPromises.stream().map(future -> future.get());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwException(Throwable exception) throws T
    {
        throw (T) exception;
    }

}

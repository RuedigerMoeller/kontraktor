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

package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.PromiseLatch;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

    public static int MAX_EXTERNAL_THREADS_POOL_SIZE = 1000; // max threads used when externalizing blocking api
    public static int DEFAULT_TIMOUT = 15000;
    public static ThreadPoolExecutor exec;
    public final static String version = "3.32.1";

    /**
     * use bounded queues if true. bounded queues block caller when they are full,
     * unfortunately this completely changes behaviour and characteristics of an actorsystem
     * and leads to hard to predict behaviour under high workload.
     * Default is to use Unbounded queues. Warning: calling queue size related
     * methods is quite expensive then (except isEmpty()).
     */

    static {
        exec = new ThreadPoolExecutor(
            MAX_EXTERNAL_THREADS_POOL_SIZE, MAX_EXTERNAL_THREADS_POOL_SIZE,
            1L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()
        );
        exec.allowCoreThreadTimeOut(true);
    }
    public static ActorsImpl instance = new ActorsImpl(); // public for testing
    public static Timer delayedCalls = new Timer();

    public static Supplier<Scheduler> defaultScheduler = () -> new SimpleScheduler();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    // constants from Callback class for convenience
    /**
     * use value as error to indicate more messages are to come (else remoting will close channel).
     */
    public static final String CONT = Callback.CONT;

    /**
     * return if given error Object signals an error or a 'complete' signal
     * @param error
     * @return
     */
    public static boolean isErrorOrComplete(Object error) {
        return ! CONT.equals(error);
    }

    public static boolean isTimeout(Object error) {
        return error instanceof Timeout;
    }

    public static boolean isComplete(Object error) {
        return error == null;
    }

    /**
     * helper to check for "special" error object "CONT". cont signals further callback results might be
     * sent (important for remoting as channels need to get cleaned up)
     * @param o
     * @return
     */
    public static boolean isCont(Object o) {
        return CONT.equals(o);
    }

    public static boolean isResult(Object error) {
        return isCont(error);
    }

    /**
     * helper to check for "special" error objects.
     * @param err
     * @return
     */
    public static boolean isError(Object err) {
        return err != null && ! CONT.equals(err);
    }

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

    public static void AddDeadLetter(String s) {
        Log.Lg.warn(null,s);
        DeadLetters().add(s);
    }

    /**
     * in case called from an actor, wraps the given interface instance into a proxy such that
     * all calls on the interface get scheduled on the calling actors thread (avoids accidental multithreading
     * when handing out callback/listener interfaces from an actor)
     *
     * if called from outside an actor thread, NOP
     *
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
    public static Queue<String> DeadLetters() {
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

    public static Actor AsUntypedActor(Actor instance) {
        return Actors.instance.newProxy(instance,Actor.class,null,-1);
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

    public static <T> IPromise<IPromise<T>[]> all(int count, Function<Integer,IPromise<T>> loop) {
        IPromise<T> promis[] = new IPromise[count];
        for ( int i = 0; i < count; i++ ) {
            promis[i] = loop.apply(i);
        }
        return all(promis);
    }

    /**
     * similar to es6 Promise.all method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once all promises provided are settled
     *
     */
    public static <T> IPromise<IPromise<T>[]> all(IPromise<T>... futures) {
        Promise res = new Promise();
        awaitSettle(futures, res);
        return res;
    }

//    public static <T> IPromise<IPromise[]> all(IPromise ... futures) {
//        Promise res = new Promise();
//        awaitSettle(futures, 0, res);
//        return res;
//    }

    /**
     * similar to es6 Promise.all method, however non-IPromise objects are not allowed
     *
     * returns a future which is settled once all promises provided are settled
     *
     */
    public static <T> IPromise<List<IPromise<T>>> all(List<IPromise<T>> futures) {
        Promise res = new Promise();
        awaitSettle(futures, res);
        return res;
    }

    /**
     * similar all but map promises to their content
     *
     * returns a future which is settled once all promises provided are settled
     *
     */
    public static <T> IPromise<List<T>> allMapped(List<IPromise<T>> futures) {
        Promise returned = new Promise();
        Promise res = new Promise();
        awaitSettle(futures, res);
        res.then( (r, e) -> {
            if (r!=null) {
                returned.resolve(((List<Promise>)r).stream().map(p -> p.get()).collect(Collectors.toList()));
            } else {
                returned.complete(null,e);
            }
        });
        return returned;
    }

    /**
     * await until all futures are settled and stream their results
     */
    public static <T> Stream<T> awaitAll(long timeoutMS, IPromise<T>... futures) {
        return streamHelper(all(futures).await(timeoutMS));
    }

    /**
     * await until all futures are settled and stream their results. Uses Actors.DEFAULT_TIMEOUT
     */
    public static <T> Stream<T> awaitAll(IPromise<T>... futures) {
        return streamHelper(all(futures).await());
    }

    public static <T> Stream<T> awaitAll(List<IPromise<T>> futures) {
        return streamHelper(all(futures).await());
    }

    public static <T> Stream<T> awaitAll(long timeoutMS, List<IPromise<T>> futures) {
        return streamHelper(all(futures).await(timeoutMS));
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
            iterator.next().then((r, e) -> {
                if (fin.compareAndSet(false, true)) {
                    p.complete(r, e);
                }
            });
        }
        return p;
    }

    /**
     * utility addition to java 8 streams
     *
     * @param t
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(T... t) {
        return Arrays.stream(t);
    }

    public static <T> KFlow<T> flow() {
        return new KFlow<>();
    }

    /**
     * abbreviation for Promise creation to make code more concise
     *
     * @param res
     * @param <T>
     * @return
     */
    public static <T> IPromise<T> resolve( T res ) {
        return new Promise<>(res);
    }

    /**
     * abbreviation for Promise creation to make code more concise
     *
     */
    public static <T> IPromise<T> reject( Object err ) {
        return new Promise<>(null,err);
    }

    /**
     * shorthand constructor
     * @param <T>
     * @return
     */
    public static <T> Promise<T> promise() {
        return new Promise<>();
    }

    /**
     * abbreviation for Promise creation to make code more concise
     *
     */
    public static <T> IPromise<T> complete( T res, Object err ) {
        return new Promise<>(res,err);
    }

    /**
     * abbreviation for Promise creation to make code more concise
     *
     */
    public static IPromise complete() {
        return new Promise<>("dummy");
    }

    /**
     * abbreviation for Promise creation to make code more concise
     *
     */
    public static IPromise resolve() {
        return new Promise<>("dummy");
    }

    /**
     * processes messages from mailbox / callbackqueue until no messages are left
     * NOP if called from non actor thread.
     */
    public static void yield() {
        yield(0);
    }

    /**
     * process messages on the mailbox/callback queue until timeout is reached. In case timeout is 0,
     * process until mailbox+callback queue is empty.
     *
     * If called from a non-actor thread, either sleep until timeout or (if timeout == 0) its a NOP.
     *
     * @param timeout
     */
    public static void yield(long timeout) {
        long endtime = 0;
        if ( timeout > 0 ) {
            endtime = System.currentTimeMillis() + timeout;
        }
        if ( Thread.currentThread() instanceof DispatcherThread ) {
            DispatcherThread dt = (DispatcherThread) Thread.currentThread();
            Scheduler scheduler = dt.getScheduler();
            boolean term = false;
            int idleCount = 0;
            while ( ! term ) {
                boolean hadSome = dt.pollQs();
                if ( ! hadSome ) {
                    idleCount++;
                    scheduler.pollDelay(idleCount);
                    if ( endtime == 0 ) {
                        term = true;
                    }
                } else {
                    idleCount = 0;
                }
                if ( endtime != 0 && System.currentTimeMillis() > endtime ) {
                    term = true;
                }
            }
        } else {
            if ( timeout > 0 ) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * only process callbacks until timeout or cbQ is empty. Messages
     * are not polled and processed. Useful in order to stall a producer,
     * but preserve message processing order.
     *
     * @param timeout
     */
    public static void yieldCallbacks(long timeout) {
        long endtime = 0;
        if ( timeout > 0 ) {
            endtime = System.currentTimeMillis() + timeout;
        }
        if ( Thread.currentThread() instanceof DispatcherThread ) {
            DispatcherThread dt = (DispatcherThread) Thread.currentThread();
            Scheduler scheduler = dt.getScheduler();
            boolean term = false;
            int idleCount = 0;
            while ( ! term ) {
                boolean hadSome = dt.pollQs(dt.POLL_CB_Q);
                if ( ! hadSome ) {
                    idleCount++;
                    scheduler.pollDelay(idleCount);
                    if ( endtime == 0 ) {
                        term = true;
                    }
                } else {
                    idleCount = 0;
                }
                if ( endtime != 0 && System.currentTimeMillis() > endtime ) {
                    term = true;
                }
            }
        } else {
            if ( timeout > 0 ) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // helper

    // changed to non recursive due to stackoverflows ..
    private static <T> void awaitSettle(final IPromise<T> futures[], final IPromise result) {
        PromiseLatch latch = new PromiseLatch(futures.length);
        latch.getPromise().then( () -> result.complete(futures, null) );
        for (int i = 0; i < futures.length; i++) {
            futures[i].then( () -> latch.countDown() );
        }
    }

    // changed to non recursive due to stackoverflows ..
    private static <T> void awaitSettle(final List<IPromise<T>> futures, final IPromise result) {
        PromiseLatch latch = new PromiseLatch(futures.size());
        latch.getPromise().then( () -> result.complete(futures, null) );
        futures.forEach( fut -> fut.then( () -> latch.countDown() ) );
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

}

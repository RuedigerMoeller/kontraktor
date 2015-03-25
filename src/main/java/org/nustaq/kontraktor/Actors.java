package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.*;
import io.jaq.mpsc.MpscConcurrentQueue;
import org.nustaq.kontraktor.util.Log;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
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
 * To change this template use File | Settings | File Templates.
 */
public class Actors {

    public static ActorsImpl instance = new ActorsImpl(); // public for testing

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

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
    public static <T extends Actor> T AsActor(Class<T> actorClazz, int qSize) {
        return (T) instance.newProxy(actorClazz, new ElasticScheduler(1), qSize);
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

    public static <T> Future<Future<T>[]> yield(Future<T> ... futures) {
        Promise res = new Promise();
        yield(futures, 0, res);
        return res;
    }

    public static <T> Future<List<Future<T>>> yield(List<Future<T>> futures) {
        Promise res = new Promise();
        yield(futures, 0, res);
        return res;
    }

    public static <T> Future<T> yieldEach(List<Future<T>> futures, Callback<T> consumer ) {
        Promise<List<Future>> prom = new Promise();
        Promise signal = new Promise();
        yield(futures, 0, prom);
        prom.onResult(list -> {
            list.forEach(fut -> consumer.settle((T) fut.getResult(), fut.getError()));
            signal.settle();
        });
        return signal;
    }

    /**
     * takes elements of a collection feeds them into the map function which is expected to return a future for each
     * element of the collection. Then waits (async) for all futures to be completed. Then iterate the collection of completed
     * futures and feed it into consumer
     * @param coll
     * @param map - then a collection item to a future (e.g. call remote method)
     * @param consumer - receives each completeed future
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public static <IN,OUT> Future yieldMap( List<IN> coll, Function<IN,Future<OUT>> map, Callback<OUT> consumer ) {
        List<Future<OUT>> collect = coll.stream().map(map).collect(Collectors.toList());
        return yieldEach(collect, consumer);
    }

    /**
     * stream settled futures unboxed. e.g. yield(f1,f2,..).then( farr -> stream(farr).forEach( val -> process(val) );
     * Note this can be used only on "settled" or "completed" futures.
     *
     * @param settledFutures
     * @param <T>
     * @return
     */
    public static <T> Stream<T> stream(Future<T> ... settledFutures) {
        return Arrays.stream(settledFutures).map(future -> future.getResult());
    }

    /**
     * similar to es6 Promise.all method, however non-Future objects are not allowed
     * @param resultArrayType
     * @param futures
     * @param <T>
     * @return
     */
    public static <T> Future<T[]> all( Class<T> resultArrayType, Future<T> ... futures ) {
        return yield(futures).then(resolved -> {
            T arr[] = (T[]) Array.newInstance(resultArrayType, futures.length);
            for (int i = 0; i < arr.length; i++) {
                arr[i] = resolved[i].getResult();
            }
            return new Promise<>(arr);
        });
    }

    /**
     * block until future returns. Warning: this can be called only from non-actor code as
     * it blocks the calling thread. If called from inside an actor, an exception is thrown.
     * Sometimes this is required/handy when setting up stuff or interoperating with
     * old-school multithreading code.
     *
     * if the future returns an error, an exception is thrown.
     *
     * @param fut
     * @param <T>
     * @return
     */
    public static <T> T sync( Future<T> fut ) {
        if ( Actor.sender.get() != null )
            throw new RuntimeException("cannot call from within actor thread");
        return unsafeSync(fut);
    }

    /**
     * use with extreme caution as if called from an actor, the actor's thread is blocked
     *
     * @param fut
     * @param <T>
     * @return
     */
    public static <T> T unsafeSync(Future<T> fut) {
        CountDownLatch latch = new CountDownLatch(1);
        fut.then( (r,e) -> latch.countDown() );
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if ( fut.getError() != null ) {
            if ( fut.getError() instanceof RuntimeException )
                throw (RuntimeException) fut.getError();
            if ( fut.getError() instanceof Throwable )
                throw new RuntimeException((Throwable) fut.getError());
            throw new RuntimeException(""+fut.getError());
        }
        return fut.getResult();
    }

    /**
     * use with extreme caution as if called from an actor, the actor's thread is blocked
     *
     * @param fut
     * @param <T>
     * @return
     */
    public static <T> T unsafeSyncThrowEx(Future<T> fut) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        fut.then( (r,e) -> latch.countDown() );
        latch.await();
        if ( fut.getError() != null ) {
            if ( fut.getError() instanceof Throwable )
                throw (Throwable) fut.getError();
            throw new RuntimeException(""+fut.getError());
        }
        return fut.getResult();
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
    public static <T> Future<Future<T>[]> async(Callable<Future<T>>... toexec) {
        return ordered(toexec, 0);
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <T> Future<Future<T>[]> ordered(final Callable<Future<T>> callables[], final int index) {
        try {
            if ( index == callables.length - 1 ) {
                return (Future<Future<T>[]>) callables[index].call();
            } else {
                Future<T> res = callables[index].call();
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

    private static <T> void yield(final Future<T> futures[], final int index, final Future result) {
        if ( index < futures.length ) {
            futures[index].then( (r,e) -> yield(futures, index + 1, result) );
        } else {
            result.settle(futures, null);
        }
    }

    private static <T> void yield(final List<Future<T>> futures, final int index, final Future result) {
        if ( index < futures.size() ) {
            futures.get(index).then((r, e) -> yield(futures, index + 1, result));
        } else {
            result.settle(futures, null);
        }
    }

}

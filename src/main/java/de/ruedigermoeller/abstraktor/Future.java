package de.ruedigermoeller.abstraktor;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 04.01.14
 * Time: 02:59
 * To change this template use File | Settings | File Templates.
 */

/**
 * wrapper anonymous actors, weaving does not work on anonymous classes
 */
public class Future<T> extends Actor {
    FutureResultReceiver rec;
    boolean autoShut;
    int responseCount;

    /**
     * Create a new future actor inheriting the dispatcher from the calling actor.
     * If a future is created from a non-actor context, a new DispatcherThread will be created,
     * which is automatically terminated after the one message has been received by the
     * future.
     * @param rec
     * @param <R>
     * @return
     */
    public static <R> Future<R> New( FutureResultReceiver<R> rec ) {
        return New(1, null, rec);
    }

    public static <R> Future<R> New( int resp, FutureResultReceiver<R> rec ) {
        return New(resp, null, rec);
    }

    /**
     * execute a piece of (possibly blocking) code in a dedicated Thread. Create a Future.New(..) to talk back from the
     * runnable. Attention, this creates a thread for each call. Use an ExecutorService in order to limit
     * the amount of threads created.
     * @param toRunIsolated
     */
    public static void Isolated( Runnable toRunIsolated ) {
        new Thread(toRunIsolated,"isolated runnable ").start();
    }

    /**
     * Create a new future actor with a dedicated Dispatcher(-Thread). The Dispatcher is automatically terminated
     * after receiving one response.
     *
     * @param rec
     * @param <R>
     * @return
     */
// MOST PROBABLY USELESS
//    public static <R> Future<R> Isolated( FutureResultReceiver<R> rec) {
//        return New(1, Actors.NewDispatcher(), rec);
//    }

    /**
     * Create a new future actor with a dedicated Dispatcher(-Thread). The Dispatcher is automatically terminated
     * after receiving "expectedResponses" responses (incl error callbacks).
     *
     * @param rec
     * @param <R>
     * @return
     */
//    public static <R> Future<R> Isolated( int expectedResponses, FutureResultReceiver<R> rec ) {
//        return New(expectedResponses, null, rec);
//    }

    public static <R> Future<R> New( int expectedResponses, Dispatcher disp, FutureResultReceiver<R> rec ) {
        // autoShutdown is not applied if a shared dispatcher is used.
        // It is always applied if disp argument != null (assume temp dispatcher). FIXME: trouble ahead in case of global future dispatcher
        disp = disp != null ? disp : Actors.threadDispatcher.get();
        boolean autoShut = Actors.threadDispatcher.get() == null && (disp == null || !disp.isSystemDispatcher());
        Future res = null;
        if ( disp != null ) {
            res = Actors.New(Future.class,disp);
        } else
            res = Actors.New(Future.class,Actors.NewDispatcher());
        res.init(rec, expectedResponses, autoShut);
        return res;
    }

    public void init(FutureResultReceiver<T> rec, int expected, boolean autoShutdown ) {
        this.rec = rec;
        autoShut = autoShutdown;
        responseCount = expected;
    }

    public void receiveObjectResult(T result) {
        rec.receiveObjectResult(result);
        respReceived();
    }

    void respReceived() {
        responseCount--;
        if ( responseCount == 0 ) {
            done();
        }
    }

    public void receiveLongResult(long result) {
        rec.receiveLongResult(result);
        respReceived();
    }

    public void receiveDoubleResult(double result) {
        rec.receiveDoubleResult(result);
        respReceived();
    }

    public void receiveError( Object error ) {
        rec.receiveError(error);
        respReceived();
    }

    @Override
    protected void finalize() throws Throwable {
        done();
    }

    public void done() {
        if ( autoShut ) {
            getDispatcher().shutDown();
        }
    }
}

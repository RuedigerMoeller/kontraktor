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

import de.ruedigermoeller.abstraktor.impl.DefaultDispatcher;

/**
 * wrapper anonymous actors, weaving does not work on anonymous classes
 */
public class Future<T> extends Actor {
    FutureResultReceiver rec;
    boolean autoShut;

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
        return New(null, rec);
    }

    public static <R> Future<R> NewIsolated(FutureResultReceiver<R> rec ) {
        Dispatcher disp = Actors.NewDispatcher();
        ((DefaultDispatcher)disp).setName("Isolated Future "+rec.getClass().getName());
        return New(disp, rec);
    }

    public static <R> Future<R> NewOther(FutureResultReceiver<R> rec ) {
        Dispatcher disp = Actors.AnyDispatcher();
        ((DefaultDispatcher)disp).setName("Isolated Future "+rec.getClass().getName());
        return New(disp, rec);
    }

    public static <R> Future<R> New( Dispatcher disp, FutureResultReceiver<R> rec ) {
        // autoShutdown is not applied if a shared dispatcher is used.
        boolean autoShut = false;
        if ( disp == null ) {
            disp = Actors.threadDispatcher.get();
        } else {
            autoShut = ! disp.isSystemDispatcher();
        }
        Future res = null;
        if ( disp != null ) {
            res = Actors.New(Future.class,disp);
        } else {
            Dispatcher newDisp = Actors.NewDispatcher();
            autoShut = true;
            ((DefaultDispatcher)newDisp).setName("Future "+rec.getClass().getName());
            res = Actors.New(Future.class, newDisp);
        }
        rec.fut = (Future) res.getActor();
        res.init(rec, autoShut);
        return res;
    }

    public void finalize() {
        done();
    }

    Thread initThread;
    public void init(FutureResultReceiver<T> rec, boolean autoShutdown ) {
        initThread = Thread.currentThread();
        this.rec = rec;
        autoShut = autoShutdown;
    }

    public void receiveObjectResult(T result) {
        if ( initThread != Thread.currentThread() ) {
            System.out.println("POK");
        }
        rec.receiveObjectResult(result);
    }

    public void receiveLongResult(long result) {
        rec.receiveLongResult(result);
    }

    public void receiveDoubleResult(double result) {
        rec.receiveDoubleResult(result);
    }

    public void receiveError( Object error ) {
        rec.receiveError(error);
    }

    public void done() {
        if ( autoShut ) {
            getDispatcher().shutDown();
        }
    }
}

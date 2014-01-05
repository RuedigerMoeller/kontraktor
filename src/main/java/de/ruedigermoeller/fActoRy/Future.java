package de.ruedigermoeller.fActoRy;

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
 * wrapper as weaving does not work on anonymous classes
 */
public class Future<T> extends Actor {
    FutureResultReceiver rec;
    boolean autoShut;
    int responseCount;

    public static <R> Future<R> New( FutureResultReceiver<R> rec ) {
        return New(1, null, rec);
    }

    public static <R> Future<R> New( FutureResultReceiver<R> rec, Dispatcher disp ) {
        return New(1, disp, rec);
    }

    public static <R> Future<R> New( int expectedResponses, FutureResultReceiver<R> rec ) {
        return New(expectedResponses, null, rec);
    }

    public static <R> Future<R> New( int expectedResponses, Dispatcher disp, FutureResultReceiver<R> rec ) {
        // autoShutdown is not applied if a shared dispatcher is used.
        // It is always applied if disp argument != null (assume temp dispatcher). FIXME: trouble ahead in case of global future dispatcher
        boolean autoShut = Actors.threadDispatcher.get() == null || disp != null ;
        Future res = null;
        if ( disp != null ) {
            res = Actors.New(Future.class,disp);
        } else
            res = Actors.New(Future.class);
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
        if ( responseCount <= 0 ) {
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

    public void done() {
        if ( autoShut ) {
            getDispatcher().shutDown();
        }
    }
}

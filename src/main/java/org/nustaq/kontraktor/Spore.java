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

import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.serialization.annotations.AnonymousTransient;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Spore is sent to a foreign actor executes on its data and sends results back to caller.
 *
 */
@AnonymousTransient
public abstract class Spore<I,O> implements Serializable, Cloneable {

    Callback cb;
    transient protected volatile boolean finished;
    transient Callback<O> localCallback;
    transient Promise finSignal = new Promise();
    transient AtomicInteger finishLatch; // in case multiple finishs are expected (sharding)

    public Spore() {
        Callback mycb = new Callback() {
            @Override
            public void complete(Object result, Object error) {
                if ( Actors.isComplete(error) ) {
                    if ( finishLatch != null ) {
                        int count = finishLatch.decrementAndGet();
                        if ( count == 0 ) {
                            finSignal.complete();
                        }
                    } else
                        finSignal.complete();
                } else {
                    if (localCallback != null) {
                        localCallback.complete((O) result, error);
                    } else {
                        System.err.println("set callback using then() prior sending");
                    }
                }
            }
        };
        this.cb = new CallbackWrapper<>(Actor.sender.get(),mycb);
    }

    public void setExpectedFinishCount(int count) {
        finishLatch = new AtomicInteger(count);
    }
    /**
     * implements code to be executed at receiver side
     * @param input
     */
    public abstract void remote( I input );

    /**
     * use local (sender side). Register and receive data streamed back from remote spore execution.
     *
     * @param cb
     */
    public Spore<I,O> setForEach(Callback<O> cb) {
        if ( localCallback != null ) {
            throw new RuntimeException("forEachResult callback handler can only be set once.");
        }
        localCallback = cb;
        return this;
    }

    public Spore<I,O> onFinish( Runnable toRun) {
        finSignal.then(toRun);
        return this;
    }

    /**
     * to be called at remote side
     * when using streaming to deliver multiple results, call this in order to signal no further
     * results are expected.
     */
    public void finish() {
        if ( finished )
            return;
        // signal finish of execution, so remoting can clean up callback id mappings
        // override if always single result or finish can be emitted by the remote method
        // note one can send FINSILENT to avoid the final message to be visible to receiver callback/spore
        cb.complete(null, null);
        finished = true;
    }

    /**
     * note that sending an error implicitely will close the backstream.
     * @param err
     */
    protected void streamError(Object err) {
        cb.complete(null, err);
    }

    protected void stream(O result) {
        cb.complete(result, Actor.CONT);
    }

    /**
     * to called from remote site. directly send stuff to remote site via callback.
     * Warning: prefer stream(), streamError() and finish().
     *
     * this can be used for generic exception catching etc. Take care to not accidentally
     * introduce multithreading ..
     *
     * Any error != 'CONT' will shut down the backchannel to remote callee afterwards.
     *
     * @param res
     * @param err
     */
    public void complete( O res, Object err ) {
        cb.complete(res,err);
    }

    /**
     * to be read at remote side in order to decide wether to stop e.g. iteration.
     * @return
     */
    public boolean isFinished() {
        return finished;
    }

    @Override
    public Spore clone() {
        try {
            return (Spore) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}

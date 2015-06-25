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

import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.serialization.util.FSTUtil;

import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * implementation of the IPromise interface.
 *
 * A Promise is unfulfilled or "unsettled" once it has not been set a result.
 * Its 'rejected' once an error has been set "reject(..)".
 * Its 'resolved' once a result has been set "resolve(..)".
 * Its 'settled' or 'completed' once a result or error has been set.
 */
public class Promise<T> implements IPromise<T> {
    protected Object result = null;
    protected Object error;
    protected Callback resultReceiver;
    // fixme: use bits
    protected volatile boolean hadResult;
    protected boolean hasFired;
    // probably unnecessary, increases cost
    // of allocation. However for now stay safe and optimize
    // from a proven-working implementation
    // note: if removed some field must set to volatile
    final AtomicBoolean lock = new AtomicBoolean(false); // (AtomicFieldUpdater is slower!)
    String id;
    IPromise nextFuture;

    /**
     * create a settled Promise by either providing an result or error.
     * @param result
     * @param error
     */
    public Promise(T result, Object error) {
        this.result = result;
        this.error = error;
        hadResult = true;
    }

    /**
     * create a resolved Promise by providing a result (cane be null).
     * @param error
     */
    public Promise(T result) {
        this(result,null);
    }

    /**
     * create an unfulfilled/unsettled Promise
     */
    public Promise() {}

    /**
     * remoting helper
     */
    public String getId() {
        return id;
    }

    /**
     * remoting helper
     */
    public Promise<T> setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * see IPromise interface
     */
    @Override
    public IPromise<T> then(Runnable result) {
        return then( (r,e) -> result.run() );
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise<T> onResult(Consumer<T> resultHandler) {
        return then( (r,e) -> {
            if ( e == null ) {
                resultHandler.accept((T) r);
            }
        });
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise<T> onError(Consumer errorHandler) {
        return then( (r,e) -> {
            if ( e != null && e != Timeout.INSTANCE) {
                errorHandler.accept(e);
            }
        });
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise<T> onTimeout(Consumer timeoutHandler) {
        return then( (r,e) -> {
            if ( e == Timeout.INSTANCE ) {
                timeoutHandler.accept(e);
            }
        });
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public <OUT> IPromise<OUT> thenAnd(final Function<T, IPromise<OUT>> function) {
        Promise res = new Promise<>();
        then( new Callback<T>() {
            @Override
            public void complete(T result, Object error) {
                if ( Actor.isError(error) ) {
                    res.complete(null, error);
                } else {
                    function.apply(result).then(res);
                }
            }
        });
        return res;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public <OUT> IPromise<OUT> then(Consumer<T> function) {
        Promise res = new Promise<>();
        then( new Callback<T>() {
            @Override
            public void complete(T result, Object error) {
                if ( Actor.isError(error) ) {
                    res.complete(null, error);
                } else {
                    function.accept(result);
                    res.complete();
                }
            }
        });
        return res;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise<T> thenAnd(Supplier<IPromise<T>> callable) {
        Promise res = new Promise<>();
        then( new Callback<T>() {
            @Override
            public void complete(T result, Object error) {
                if ( Actor.isError(error) ) {
                    res.complete(null, error);
                } else {
                    IPromise<T> call = null;
                    call = callable.get().then(res);
                }
            }
        });
        return res;
    }


    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public <OUT> IPromise<OUT> catchError(final Function<Object, IPromise<OUT>> function) {
        Promise res = new Promise<>();
        then( new Callback<T>() {
            @Override
            public void complete(T result, Object error) {
                if ( ! Actor.isError(error) ) {
                    res.complete(null, error);
                } else {
                    function.apply(error).then(res);
                }
            }
        });
        return res;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public <OUT> IPromise<OUT> catchError(Consumer<Object> function) {
        Promise res = new Promise<>();
        then( new Callback<T>() {
            @Override
            public void complete(T result, Object error) {
                if ( ! Actor.isError(error) ) {
                    res.complete(null, error);
                } else {
                    function.accept(error);
                    res.complete();
                }
            }
        });
        return res;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    public void timedOut( Timeout to ) {
        if (!hadResult ) {
            complete(null, to);
        }
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise then(Callback resultCB) {
        // FIXME: this can be implemented more efficient
        while( !lock.compareAndSet(false,true) ) {}
        try {
            if (resultReceiver != null)
                throw new RuntimeException("Double register of promise listener");
            resultReceiver = resultCB;
            if (hadResult) {
                hasFired = true;
                if (nextFuture == null) {
                    nextFuture = new Promise(result, error);
                    lock.set(false);
                    resultCB.complete(result, error);
                } else {
                    lock.set(false);
                    resultCB.complete(result, error);
                    nextFuture.complete(result, error);
                    return nextFuture;
                }
            }
            if (resultCB instanceof IPromise) {
                return (IPromise) resultCB;
            }
            lock.set(false);
            while( !lock.compareAndSet(false,true) ) {}
            if (nextFuture == null) {
                return nextFuture = new Promise();
            } else {
                return nextFuture;
            }
        } finally {
            lock.set(false);
        }
    }

    /**
     * special method for tricky things. Creates a nextFuture or returns it.
     * current
     * @return
     */
    public Promise getNext() {
        while( !lock.compareAndSet(false,true) ) {}
        try {
            if (nextFuture == null)
                return new Promise();
            else
                return (Promise) nextFuture;
        } finally {
            lock.set(false);
        }
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    public Promise getLast() {
        while( !lock.compareAndSet(false,true) ) {}
        try {
            if (nextFuture == null)
                return this;
            else
                return ((Promise)nextFuture).getLast();
        } finally {
            lock.set(false);
        }
    }

    /**
     * same as then, but avoid creation of new promise
     * @param resultCB
     */
    public void finallyDo(Callback resultCB) {
        // FIXME: this can be implemented more efficient
        while( !lock.compareAndSet(false,true) ) {}
        try {
            if (resultReceiver != null)
                throw new RuntimeException("Double register of future listener");
            resultReceiver = resultCB;
            if (hadResult) {
                hasFired = true;
                resultCB.complete(result, error);
            }
        } finally {
            lock.set(false);
        }
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public final void complete(Object res, Object error) {
        this.result = res;
        Object prevErr = this.error;
        this.error = error;
        while( !lock.compareAndSet(false,true) ) {}
        try {
            if (hadResult) {
                if ( prevErr instanceof Timeout ) {
                    this.error = prevErr;
                    return;
                }
                throw new RuntimeException("Double result received on future " + prevErr );
            }
            hadResult = true;
            if (resultReceiver != null) {
                if (hasFired) {
                    lock.set(false);
                    throw new RuntimeException("Double fire on callback");
                }
                hasFired = true;
                resultReceiver.complete(result, error);
                resultReceiver = null;
                while (!lock.compareAndSet(false, true)) {
                }
                if (nextFuture != null) {
                    nextFuture.complete(result, error);
                }
                return;
            }
        } finally {
            lock.set(false);
        }
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public T get() {
        return (T) result;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public T await(long timeoutMillis) {
        awaitPromise(timeoutMillis);
        return awaitHelper();
    }

    /**
     * see IPromise (inheriting Callback) interface
     * @param timeout
     */
    @Override
    public IPromise<T> awaitPromise(long timeout) {
        long endtime = 0;
        if ( timeout > 0 ) {
            endtime = System.currentTimeMillis() + timeout;
        }
        if ( Thread.currentThread() instanceof DispatcherThread ) {
            DispatcherThread dt = (DispatcherThread) Thread.currentThread();
            Scheduler scheduler = dt.getScheduler();
            int idleCount = 0;
            dt.__stack.add(this);
            while( ! isSettled() ) {
                if ( ! dt.pollQs() ) {
                    idleCount++;
                    scheduler.pollDelay(idleCount);
                } else {
                    idleCount = 0;
                }
                if ( endtime != 0 && System.currentTimeMillis() > endtime && ! isSettled() ) {
                    timedOut(Timeout.INSTANCE);
                    break;
                }
            }
            dt.__stack.remove(dt.__stack.size()-1);
            return this;
        } else {
            // if outside of actor machinery, just block
            CountDownLatch latch = new CountDownLatch(1);
            then( (res, err) -> {
                latch.countDown();
            });
            boolean timedOut = false;
            try {
                timedOut = ! latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if ( timedOut )
                timedOut(Timeout.INSTANCE);
            return this;
        }
    }

    private T awaitHelper() {
        if ( Actor.isError(getError()) ) {
            if ( getError() instanceof Throwable ) {
                FSTUtil.<RuntimeException>rethrow((Throwable) getError());
                return null; // never reached
            }
            else {
                if ( getError() == Timeout.INSTANCE ) {
                    throw new KTimeoutException();
                }
                throw new AwaitException(getError());
            }
        } else {
            return get();
        }
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public IPromise timeoutIn(long millis) {
        final Actor actor = Actor.sender.get();
        if ( actor != null )
            actor.delayed(millis, ()-> timedOut(Timeout.INSTANCE));
        else {
            Actors.delayedCalls.schedule( new TimerTask() {
                @Override
                public void run() {
                    timedOut(Timeout.INSTANCE);
                }
            },millis);
        }
        return this;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public Object getError() {
        return error;
    }

    /**
     * see IPromise (inheriting Callback) interface
     */
    @Override
    public boolean isSettled() {
        return hadResult;
    }


    // debug
    public boolean _isHadResult() {
        return hadResult;
    }

    // debug
    public boolean _isHasFired() {
        return hasFired;
    }

    @Override
    public String toString() {
        return "Result{" +
            "result=" + result +
            ", error=" + error +
            '}';
    }
}

package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Copyright (c) 2014, Ruediger Moeller. All rights reserved.
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
 * Date: 03.01.14
 * Time: 18:46
 */

/**
 * ..
 */
public class CallbackWrapper<T> implements Future<T>, Serializable {

    static Method receiveRes;

    static {
        try {
            receiveRes = Callback.class.getMethod("settle", new Class[]{Object.class,Object.class});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    final Actor targetActor;
    Callback<T> realCallback;

    public CallbackWrapper(Actor targetQ, Callback<T> realFuture) {
        this.realCallback = realFuture;
        this.targetActor = targetQ;
    }

    @Override
    public void settle(T result, Object error) {
        if ( realCallback == null ) {
            return;
        }
        if ( targetActor == null ) {
            // call came from outside the actor world => use current thread => blocking the callback blocks actor, dont't !
            try {
                receiveRes.invoke(realCallback, result, error);
            } catch (Exception e) {
                Log.Warn( this, e, "" );
            }
        } else {
            CallEntry ce = new CallEntry( realCallback, receiveRes, new Object[]{result,error}, Actor.sender.get(), targetActor, true);
            targetActor.__scheduler.put2QueuePolling(targetActor.__cbQueue, true, ce, targetActor);
        }
    }

    @Override
    public Future<T> then(Runnable result) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(result);
    }

    @Override
    public Future<T> then(Supplier<Future<T>> result) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(result);
    }

    @Override
    public Future then(Callback<T> result) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(result);
    }

    @Override
    public Future<T> onResult(Consumer<T> resultHandler) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).onResult(resultHandler);
    }

    @Override
    public Future<T> onError(Consumer errorHandler) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).onError(errorHandler);
    }

    @Override
    public Future<T> onTimeout(Consumer timeoutHandler) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).onTimeout(timeoutHandler);
    }

    @Override
    public <OUT> Future<OUT> then(Function<T, Future<OUT>> function) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(function);
    }

    @Override
    public <OUT> Future<OUT> then(Consumer<T> function) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(function);
    }

    @Override
    public <OUT> Future<OUT> catchError(Function<Object, Future<OUT>> function) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).catchError(function);
    }

    @Override
    public <OUT> Future<OUT> catchError(Consumer<Object> function) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).catchError(function);
    }

    @Override
    public T getResult() {
        if (realCallback instanceof Future == false)
            return null;
        else
            return (T) ((Future)realCallback).getResult();
    }

    @Override
    public Object getError() {
        if (realCallback instanceof Future == false)
            return null;
        else
            return (T) ((Future)realCallback).getError();
    }

    @Override
    public Future timeoutIn(long millis) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException( "currently supported for futures only" );
        ((Future)realCallback).timeoutIn(millis);
        return this;
    }

    @Override
    public boolean isCompleted() {
        if (realCallback instanceof Future == false)
            throw new RuntimeException( "currently supported for futures only" );
        else
            return ((Future)realCallback).isCompleted();
    }
}

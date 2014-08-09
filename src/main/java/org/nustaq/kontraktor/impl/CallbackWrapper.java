package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Filter;
import org.nustaq.kontraktor.Future;

import java.io.Serializable;
import java.lang.reflect.Method;

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
            receiveRes = Callback.class.getMethod("receiveResult", new Class[]{Object.class,Object.class});
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
    public void receiveResult(T result, Object error) {
        if ( realCallback == null ) {
            return;
        }
        if ( targetActor == null ) {
            // call came from outside the actor world => use current thread => blocking the callback blocks actor, dont't !
            try {
                receiveRes.invoke(realCallback, result, error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            CallEntry ce = new CallEntry( realCallback, receiveRes, new Object[]{result,error}, Actor.sender.get(), targetActor);
            targetActor.__scheduler.put2QueuePolling(targetActor.__cbQueue, ce);
        }
    }

    @Override
    public Future then(Callback<T> result) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).then(result);
    }

    @Override
    public <OUT> Future<OUT> map(Filter<T, OUT> filter) {
        if (realCallback instanceof Future == false)
            throw new RuntimeException("this is an error.");
        else
            return ((Future)realCallback).map(filter);
    }

    @Override
    public T getResult() {
        if (realCallback instanceof Future == false)
            return null;
        else
            return (T) ((Future)realCallback).getResult();
    }

    @Override
    public void signal() {
        receiveResult(null,null);
    }
}

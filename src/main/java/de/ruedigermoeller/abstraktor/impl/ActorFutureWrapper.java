package de.ruedigermoeller.abstraktor.impl;

import de.ruedigermoeller.abstraktor.ActorFuture;
import de.ruedigermoeller.abstraktor.ActorProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by ruedi on 05.05.14.
 */
public class ActorFutureWrapper<T> extends ActorFuture<T> {

    static Method receiveErr;
    static Method receiveRes;

    static {
        try {
            receiveErr = ActorFuture.class.getMethod("receiveError", new Class[]{Object.class});
            receiveRes = ActorFuture.class.getMethod("receiveResult", new Class[]{Object.class});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    final Dispatcher dispatcher;
    ActorFuture<T> realFuture;

    public ActorFutureWrapper(Dispatcher dispatcher, ActorFuture<T> realFuture) {
        this.realFuture = realFuture;
        this.dispatcher = dispatcher;
    }

    @Override
    public void receiveError(Object error) {
        if ( dispatcher == null ) {
            // call came from outside the actor world => use current thread => blocking the callback blocks actor, dont't !
            try {
                receiveErr.invoke(realFuture, error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            int count = 0;
            while (dispatcher.dispatchCallback(realFuture, receiveErr, new Object[]{error})) {
                dispatcher.yield(count++);
            }
        }
    }

    @Override
    public void receiveResult(T result) {
        if ( dispatcher == null ) {
            // call came from outside the actor world => use current thread => blocking the callback blocks actor, dont't !
            try {
                receiveRes.invoke(realFuture, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            int count = 0;
            while (dispatcher.dispatchCallback(realFuture, receiveRes, new Object[]{result})) {
                dispatcher.yield(count++);
            }
        }
    }
}

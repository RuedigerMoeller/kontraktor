package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.impl.CallbackWrapper;
import de.ruedigermoeller.kontraktor.impl.ActorProxyFactory;
import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.lang.reflect.*;
import java.util.concurrent.*;

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

    static Actors instance = new Actors();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    public static void SetDefaultQueueSize(int siz) {
        DispatcherThread.DEFAULT_QUEUE_SIZE = siz;
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz,-1);
    }

    /**
     * create an new actor. If this is called outside an actor, a new DispatcherThread will be scheduled. If
     * called from inside actor code, the new actor will share the thread+queue with the caller.
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, int qSize) {
        return (T) instance.newProxy(actorClazz,qSize);
    }

    /**
     * Creates a wrapper on the given object enqueuing all calls to INTERFACE methods of the given object to the calling actors's queue.
     * This is used to enable processing of resulting callback's in the callers thread.
     * see also @InThread annotation.
     * @param callback
     * @param <T>
     * @return
     */
    public static <T> T InThread(T callback) {
        Class<?>[] interfaces = callback.getClass().getInterfaces();
        InvocationHandler invoker = DispatcherThread.getThreadDispatcher().getInvoker(callback);
        if ( invoker == null ) // called from outside actor world
        {
            return callback; // callback in callee thread
        }
        return (T) Proxy.newProxyInstance(callback.getClass().getClassLoader(), interfaces, invoker);
    }

    /**
     * create an new actor dispatched in the given DispatcherThread
     *
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, DispatcherThread disp) {
        return (T) instance.newProxy(actorClazz,disp);
    }

    /**
     * create a new actor with a newly created DispatcherThread
     * @param actorClazz
     * @param <T>
     * @return
     */
    public static <T extends Actor> T SpawnActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz, instance.newDispatcher(-1) );
    }

    /**
     * create a new actor with a newly created DispatcherThread
     * @param actorClazz
     * @param <T>
     * @param qSiz - size of mailbox queue
     * @return
     */
    public static <T extends Actor> T SpawnActor(Class<? extends Actor> actorClazz, int qSiz) {
        return (T) instance.newProxy(actorClazz, instance.newDispatcher(qSiz) );
    }

    public static <T> void Execute( final Callable<T> toCall, Callback<T> resultHandler ) {
        instance.runBlockingCall(toCall,resultHandler);
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected ExecutorService exec = Executors.newCachedThreadPool();

    protected Actors() {
        factory = new ActorProxyFactory();
    }

    protected ActorProxyFactory factory;

    protected ActorProxyFactory getFactory() {
        return factory;
    }

    protected <T> void runBlockingCall( final Callable<T> toCall, Callback<T> resultHandler ) {
        final CallbackWrapper<T> resultWrapper = new CallbackWrapper<>(DispatcherThread.getThreadDispatcher(),resultHandler);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    resultWrapper.receiveResult(toCall.call(),null);
                } catch (Throwable th) {
                    resultWrapper.receiveResult(null, th);
                }
            }
        });
    }

    protected Actor newProxy(Class<? extends Actor> clz, DispatcherThread disp ) {
        try {
            Actor res = clz.newInstance();
            res.dispatcher = disp;
            Actor proxy = getFactory().instantiateProxy(res);
            Actor yieldProxy = getFactory().instantiateProxy(res);
            yieldProxy.__isYield = true;

            res.__self = proxy;
            proxy.__yield = yieldProxy;
            res.__yield = yieldProxy;

            disp.actorAdded(res);
            return proxy;
        } catch (Exception e) {
            if ( e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    protected Actor newProxy(Class<? extends Actor> clz, int qsize) {
        if ( DispatcherThread.getThreadDispatcher() != null ) {
            return newProxy( clz, DispatcherThread.getThreadDispatcher() );
        } else {
            try {
                return newProxy(clz, newDispatcher(qsize));
            } catch (Exception e) {
                if ( e instanceof RuntimeException)
                    throw (RuntimeException)e;
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * return a new dispatcher backed by a new thread. Overriding classes should *not*
     * return existing dispatchers here, as this can be used to isolate blocking code from the actor flow.
     *
     * if qSiz lesser or equal 0 use default size
     * @return
     */
    protected DispatcherThread newDispatcher(int qSize) {
        return new DispatcherThread(qSize);
    }

}

package org.nustaq.kontraktor;

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
 * Date: 03.01.14
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.impl.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Baseclass for actor implementations. Note that actors are not created using constructors.
 * Use Actors.AsActor(..) or Actor.SpawnActor() to instantiate an actor instance. To pass initialization parameter,
 * define an init method in your implementation and call it from the instantiating instance.
 *
 * e.g.; MyActor act = Actors.Channel(MyActor.class); act.myInit( x,y,z );
 *
 * The init method then will be executed in the thread of the dispatcher associated with your
 * actor avoiding problems rised by state visibility inconsistency amongst threads.
 *
 * Inside an actor, everything is executed single threaded. You don't have to worry about synchronization.
 *
 * All 'messages' of an actor are defined by 'public void' methods.
 * Actor methods are not allowed to return values. They must be of type void. Pass a Callback as argument to a call
 * in order to receive results from other actors/threads.
 * Non public methods can be called from inside the actor, but not outside as a message.
 *
 * Note that you have to pass immutable objects as arguments, else you'll get unpredictable behaviour.
 *
 * Code inside an actor is not allowed to ever block the current thread (networking etc.).
 * Use Actors.Exec in case you need to do blocking calls (e.g. synchronous requests)
 *
 */
public class Actor<SELF extends Actor> implements Serializable {

    public static ThreadLocal<Actor> sender = new ThreadLocal<>();

    // internal ->
    public Queue __mailbox;
    public Queue __cbQueue;
    public Thread __currentDispatcher;
    public Scheduler __scheduler;
    public boolean __stopped = false;
    public long __nanos;
    public Actor __self;
    public int __remoteId;
//    public Object __remotingImpl; a list is required to be notified on stop
    // <- internal

    /**
     * required by bytecode magic. Use Actors.Channel(..) to construct actor instances
     */
    public Actor() {
    }

    /**
     * use this to call public methods using actor-dispatch instead of direct in-thread call.
     * Important: When passing references out of your actor, always pass 'self()' instead of this !
     * @return
     */
    protected SELF self() {
        return (SELF)__self;
    }

    public ActorProxyFactory getFactory() {
        return Actors.instance.getFactory();
    }

    /**
     * @return if this is an actorproxy, return the underlying actor instance, else return this
     */
    public SELF getActor() {
        return (SELF) this;
    }

    /**
     * $$stop receiving events. If there are no actors left on the underlying dispatcher,
     * the dispatching thread will be terminated.
     */
    public void $stop() {
        throw ActorStoppedException.Instance;
    }

    @CallerSideMethod public boolean isProxy() {
        return getActor() != this;
    }

    protected Future<Future[]> yield(Future... futures) {
        return __scheduler.yield(futures);
    }

    protected <T> Future<List<Future<T>>> yieldList( List<Future<T>> futures) {
        return __scheduler.yield(futures);
    }

    /**
     * execute a callable asynchronously (in a different thread) and return a future
     * of the result (delivered in caller thread)
     * @param callable
     * @param <T>
     * @return
     */
    protected <T> Future<T> async(Callable<T> callable) {
        Promise<T> prom = new Promise<>();
        __scheduler.runBlockingCall(self(),callable,prom);
        return prom;
    }

    @CallerSideMethod
    protected <T> T inThread(Actor proxy, T cbInterface) {
        return __scheduler.inThread(proxy, cbInterface);
    }

    /**
     * schedule an action or call delayed.
     * typical use case:
     * delayed( 100, () -> { self().doAction( x, y,  ); } );
     *
     */
    protected void delayed( int millis, final Runnable toRun ) {
        __scheduler.delayedCall( millis, inThread(self(),toRun) );
    }

    @CallerSideMethod public Scheduler getScheduler() {
        return __scheduler;
    }

    /**
     * @return an estimation on the queued up entries in the mailbox. Can be used for bogus flow control
     */
    public int getMailboxSize() {
        if ( ! isProxy() )
            return self().getMailboxSize();
        return __mailbox.size();
    }

    /**
     * @return an estimation on the queued up callback entries. Can be used for bogus flow control.
     */
    public int getCallbackSize() {
        if ( ! isProxy() )
            return self().getCallbackSize();
        return __cbQueue.size();
    }

    Thread _t;
    protected final void checkThread() {
        if (_t==null) {
            _t = Thread.currentThread();
        } else {
            if ( _t != Thread.currentThread() ) {
                throw new RuntimeException("Wrong Thread");
            }
        }
    }

    @CallerSideMethod public Actor getActorRef() {
        return __self;
    }

    @CallerSideMethod public boolean isRemote() {
        return __remoteId != 0;
    }

////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    // dispatch an outgoing call to the target actor queue. Runs in Caller Thread
    @CallerSideMethod public Object __dispatchCall( Actor receiver, String methodName, Object args[] ) {
        if ( __stopped )
            throw new RuntimeException("Actor "+this+" received message after being stopped "+methodName );
        return __scheduler.dispatchCall(sender.get(), receiver, methodName, args);
    }

    ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    @CallerSideMethod public Method __getCachedMethod(String methodName, Actor actor) {
        Method method = methodCache.get(methodName);
        if ( method == null ) {
            Method[] methods = actor.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if ( m.getName().equals(methodName) ) {
                    methodCache.put(methodName,m);
                    method = m;
                    break;
                }
            }
        }
        return method;
    }


}

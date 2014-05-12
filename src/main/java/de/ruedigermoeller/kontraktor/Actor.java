package de.ruedigermoeller.kontraktor;

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

import de.ruedigermoeller.kontraktor.annotations.CallerSideMethod;
import de.ruedigermoeller.kontraktor.impl.ActorProxyFactory;
import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.*;

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
public class Actor {

    public Actor __self;       // internal use

    DispatcherThread dispatcher;

    /**
     * required by bytecode magic. Use Actors.Channel(..) to construct actor instances
     */
    public Actor() {
    }

    /**
     * @return the DispatcherThread of this actor
     */
    public DispatcherThread getDispatcher() {
        if ( __self == null ) {
            return getActor().getDispatcher();
        }
        return dispatcher;
    }

    /**
     * use this to call public methods using actor-dispatch instead of direct in-thread call.
     * Improtant: When passing references out of your actor, always pass 'self()' instead of this !
     * @param <T>
     * @return
     */
    protected <T extends Actor> T self() {
        return (T)__self;
    }

    public ActorProxyFactory getFactory() {
        return Actors.instance.getFactory();
    }

    /**
     * @return if this is an actorproxy, return the underlying actor instance, else return this
     */
    public Actor getActor() {
        return this;
    }

    /**
     * blocks calling thread until all messages on this actor have been processed
     */
    @CallerSideMethod public void sync() {
        if ( __self == null ) {
            getActor().sync();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        __self.__sync(latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * stop receiving events. If there are no actors left on the underlying dispatcher,
     * the dispatching thread will be terminated.
     */
    @CallerSideMethod public void stop() {
        getDispatcher().actorStopped(this);
    }

    ////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    @CallerSideMethod public void __dispatcher( DispatcherThread d ) {
        dispatcher = d;
    }

    @CallerSideMethod public void __sync(CountDownLatch latch) {
        latch.countDown();
    }

    protected ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    // try to offer an outgoing call to the target actor queue. Runs in Caller Thread
    @CallerSideMethod public void __dispatchCall( ActorProxy receiver, String methodName, Object args[] ) {
        // System.out.println("dispatch "+methodName+" "+Thread.currentThread());
        // here sender + receiver are known in a ST context
        Method method = methodCache.get(methodName);
        Actor actor = receiver.getActor();
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
        int count = 0;
        while ( actor.getDispatcher().dispatch(receiver, method, args) ) {
            DispatcherThread.yield(count++);
        }
    }

}

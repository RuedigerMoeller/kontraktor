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
 * Date: 03.01.14
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */

import de.ruedigermoeller.abstraktor.impl.ActorProxyFactory;
import de.ruedigermoeller.abstraktor.impl.Dispatcher;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Baseclass for actor implementations. Note that actors are not created using constructors.
 * Use Actors.Channel(..) to instantiate an actor instance. To pass initialization parameter,
 * define an init method in your implementation.
 *
 * e.g.; MyActor act = Actors.Channel(MyActor.class); act.myInit( x,y,z );
 *
 * The init method then will be executed in the thread of the dispatcher associated with your
 * actor avoiding problems rised by state visibility inconsistency amongst threads.
 *
 * Inside an actor, everything is executed single threaded. You don't have to worry about synchronization.
 *
 * public actor methods are not allowed to return values. They must be of type void. Pass a ChannelActor.Channel to a call
 * in order to receive results from other actors. This does not apply to non-public methods, as they cannot be called
 * from outside the actor.
 *
 * Note that you have to pass immutable objects as arguments, else you'll get unpredictable behaviour.
 * ChannelActor versions will provide a generic deep copy for those cases, until then fall back to serialization if you
 * really have to copy large object graphs (you also might mix in traditional synchronization for shared structure access).
 *
 * code inside an actor is not allowed to ever block the current thread (networking etc.). Use ChannelActor.Channel and executors
 * talking to the future result handler in order to handle calls to blocking code.
 *
 */
public class Actor {

    public Actor __self;       // internal use

    Dispatcher dispatcher;

    /**
     * required by bytecode magic. Use Actors.Channel(..) to construct actor instances
     */
    public Actor() {
    }

    public Dispatcher getDispatcher() {
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

    public Actor getActor() {
        return this;
    }

    public void sync() {
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

    ////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    public void __dispatcher( Dispatcher d ) {
        dispatcher = d;
    }

    public void __sync(CountDownLatch latch) {
        latch.countDown();
    }

    HashMap<String, Method> methodCache = new HashMap<>();

    /**
     * callback from bytecode weaving
     */
    public boolean __doDirectCall(String methodName, ActorProxy proxy) {
        return false; //proxy.getActor().__outCalls == 0;
    }

    // try to offer an outgoing call to the target actor queue. Runs in Caller Thread
    public void __dispatchCall( ActorProxy receiver, boolean sameThread, String methodName, Object args[] ) {
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
        while ( actor.getDispatcher().dispatch(receiver, sameThread, method, args) ) {
            Dispatcher.yield(count++);
        }
    }

}

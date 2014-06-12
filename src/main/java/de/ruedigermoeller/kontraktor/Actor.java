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
import de.ruedigermoeller.kontraktor.impl.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
public class Actor<SELF extends Actor> {

    public static ThreadLocal<List<Message>> methodSequence = new ThreadLocal<List<Message>>() {
        @Override protected List<Message> initialValue() { return new ArrayList<>(); }
    };

    public static MessageSequence currentSequence() {
        List<Message> res = methodSequence.get();
        methodSequence.set(new ArrayList<Message>());
        return new MessageSequence(res);
    }

    public static Message currentMsg() {
        return currentSequence().first();
    }

    public static Message msg( Future call ) {
        return currentSequence().first();
    }

    public static MessageSequence seq( Future... calls ) {
        List<Message> res = methodSequence.get();
        methodSequence.set(new ArrayList<Message>());
        return new MessageSequence(res);
    }

    // internal
    public Queue __mailbox;
    public Queue __cbQueue;
    public Thread __currentDispatcher;

    public long __nanos;
    public Actor __self;
    public Actor __seq;
    public boolean __isSeq = false;

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

    public SELF $() { return (SELF) __seq; }

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
     * stop receiving events. If there are no actors left on the underlying dispatcher,
     * the dispatching thread will be terminated.
     */
    @CallerSideMethod public void stop() {
        throw new RuntimeException("fixme");
//        getDispatcher().actorStopped(this);
    }

    public void executeInActorThread( ActorRunnable toRun, Callback cb ) {
        toRun.run(getActorAccess(), getActor(), cb);
    }

    protected Object getActorAccess() {
        return null;
    }

    public boolean isProxy() {
        return getActor() != this;
    }

    protected Future<Future[]> yield(Future... futures) {
        return Actors.Yield(futures);
    }

    protected <T> Future<T> async(Callable<T> callable) {
        return Actors.Async(this,callable);
    }

    ////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    // dispatch an outgoing call to the target actor queue. Runs in Caller Thread
    @CallerSideMethod public Object __dispatchCall( Actor receiver, String methodName, Object args[] ) {
        return DispatcherThread.DispatchCall(this, receiver,methodName, args);
    }

}

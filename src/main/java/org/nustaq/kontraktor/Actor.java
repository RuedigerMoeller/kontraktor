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
import java.util.concurrent.ConcurrentLinkedQueue;

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

    // constants from Callback class for convenience

    /**
     * use value to signal no more messages. THE RECEIVER CALLBACK WILL NOT SEE THIS MESSAGE.
     */
    public static final String FINSILENT = Callback.FINSILENT;
    /**
     * use value as error to indicate more messages are to come (else remoting will close channel).
     */
    public static final String CONT = Callback.CONT;
    /**
     * use this value to signal no more messages. The receiver callback will receive the message.
     * Note that any value except CONT will also close the callback channel. So this is informal.
     */
    public static final String FIN = Callback.FIN;

    public static boolean Fin(Object o) {
        return FIN.equals(o) || FINSILENT.equals(o) || o == null;
    }
    public static boolean Cont(Object o) {
        return CONT.equals(o);
    }


    public static ThreadLocal<Actor> sender = new ThreadLocal<>();

    // internal ->
    public Queue __mailbox;
    public Queue __cbQueue;
    public Thread __currentDispatcher;
    public Scheduler __scheduler;
    public volatile boolean __stopped = false;
    public long __nanos;
    public Actor __self;
    public int __remoteId;
    public volatile ConcurrentLinkedQueue<RemoteConnection> __connections; // a list of connection required to be notified on close
    // register callbacks notified on stop
    ConcurrentLinkedQueue<Callback<SELF>> __stopHandlers;
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
        __stop();
    }

    @CallerSideMethod public boolean isStopped() {
        return __stopped;
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
    protected void delayed( long millis, final Runnable toRun ) {
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

    /**
     * closes associated remote connection(s) if present. NOP otherwise.
     */
    public void $close() {
        if (__connections != null) {
            final ConcurrentLinkedQueue<RemoteConnection> prevCon = __connections;
            __connections = null;
            prevCon.forEach((con) -> con.close());
        }
    }

    /**
     * avoids exception when closing an actor after stop has been called.
     */
    @CallerSideMethod public void stopSafeClose() {
        if ( isStopped() ) {
            getActor().$close(); // is threadsafe
        } else {
            self().$close();
        }
    }

    /**
     * can be used to wait for all messages having been processed
     * @return
     */
    public Future $sync() {
        return new Promise<>("void");
    }

////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    @CallerSideMethod public void __addStopHandler( Callback<SELF> cb ) {
        if ( __stopHandlers == null ) {
            getActorRef().__stopHandlers = new ConcurrentLinkedQueue();
            getActor().__stopHandlers = getActorRef().__stopHandlers;
        }
        __stopHandlers.add(cb);
    }

    @CallerSideMethod public void __addRemoteConnection( RemoteConnection con ) {
        if ( __connections == null ) {
            getActorRef().__connections = new ConcurrentLinkedQueue<RemoteConnection>();
            getActor().__connections = getActorRef().__connections;
        }
        __connections.add(con);
    }

    @CallerSideMethod public void __removeRemoteConnection( RemoteConnection con ) {
        if ( __connections != null ) {
            __connections.remove(con);
        }
    }

    @CallerSideMethod public void __stop() {
        if ( getActorRef().isStopped() && getActor().isStopped() )
            return;
        getActorRef().__stopped = true;
        getActor().__stopped = true;
        if (__stopHandlers!=null) {
            __stopHandlers.forEach( (cb) -> cb.receive(self(), null) );
            __stopHandlers.clear();
        }
        throw ActorStoppedException.Instance;
    }

    // dispatch an outgoing call to the target actor queue. Runs in Caller Thread
    @CallerSideMethod public Object __enqueueCall( Actor receiver, String methodName, Object args[] ) {
        if ( __stopped ) {
            if ( methodName.equals("$stop") ) // ignore double stop
                return null;
            String senderString = sender.get() == null ? "null" : sender.get().getClass().getName();
            String s = "DEAD LETTER: sender:" + senderString + " receiver::msg:" + receiver.getClass().getSimpleName() + "::" + methodName;
            s = s.replace("_ActorProxy","");
            Actors.AddDeadLetter(s);
//            throw new RuntimeException("Actor " + this + " received message after being stopped " + methodName);
        }
        return __scheduler.enqueueCall(sender.get(), receiver, methodName, args);
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

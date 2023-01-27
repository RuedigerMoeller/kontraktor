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
 */

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.TicketMachine;
import org.nustaq.serialization.util.FSTUtil;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Baseclass for actor/eventloop implementations. Note that actors are not created using constructors.
 * Use Actors.AsActor(..) to instantiate an actor instance.
 *
 * Example (public methods are automatically transformed to be async):
 * <pre>
 * public class MyActor extends Actor<MyActor> {
 *
 *     // public async API
 *     public IPromise init() {..}
 *     public void asyncMessage(String arg) { .. }
 *     public IPromise asyncMessage(String arg) { .. }
 *     public void asyncMessage(int arg, Callback aCallback) { .. }
 *
 *     // synchronous methods (safe as cannot be called by foreign threads)
 *     protected String syncMethod() { .. }
 * }
 *
 * MyActor act = Actors.AsActor(MyActor.class);
 * act.init().then( () -> { System.out.println("done"); }
 * Object res = act.asyncMessage("Hello").await();
 *
 * </pre>
 *
 * Note that unlike in other actor libraries, processing of Callback and Promise is transfered to the
 * current actor thread, so its safe to close over actor state.
 */
public class Actor<SELF extends Actor> extends Actors implements Serializable, Monitorable, Executor {

    /**
     * contains sender of a message if one actor messages to another actor
     */
    public static ThreadLocal<Actor> sender = new ThreadLocal<>();

    /**
     * contains remote connection if current message came from remote
     */
    public static ThreadLocal<ConnectionRegistry> connection = new ThreadLocal<>();

    /**
     * @return current actor or throw an exception if not running inside an actor thread.
     */
    public static Actor current() {
        Actor actor = sender.get();
        if ( actor == null )
            throw new MustBeRunFromActorThread();
        return actor;
    }

    public static boolean inside() {
        return sender.get() != null;
    }

    /**
     * free for outer mechanics to use.
     */
    public Object userData; // attention self() and this !!

    // internal ->
    public Queue __mailbox; // mailbox/eventloop queue
    public int __mbCapacity;
    public Queue __cbQueue; // queue of callbacks/future results
    public Thread __currentDispatcher; // thread of this actor
    public Scheduler __scheduler;
    public volatile boolean __stopped;
    public Actor __self; // the proxy object
    private Map<String,Runnable> _debounceMap;
    public long __remoteId; // id in case this actor is published via network
    public volatile ConcurrentLinkedQueue<ConnectionRegistry> __connections; // a list of connections required to be notified on close (publisher/server side))
    public ConnectionRegistry __clientConnection; // remoteconnection in case this is a remote ref
    public boolean zzRoutingGCEnabled;
    // register callbacks notified on stop
    ConcurrentLinkedQueue<Callback<SELF>> __stopHandlers;
    public int __mailboxCapacity; // fixme: checkout why there is also _mbCapacity :)
    public void __submit(Runnable toRun) { toRun.run(); }
    // <- internal

    /**
     * required by bytecode magic. Use Actors.AsActor(..) to construct actor instances
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
     * Note: gets patched during proxy generation to guarantee correctness.
     */
    public SELF getActor() {
        return (SELF) this;
    }

    /**
     * stop receiving events. If there are no actors left on the underlying dispatcher,
     * the dispatching thread will be terminated.
     */
    @CallerSideMethod public void stop() {
        if ( isRemote() ) {
            throw new RuntimeException("Cannot stop remote ref");
        }
        SELF self = self();
        if ( self != null ) {
            self.ping().then(() -> {
                SELF selfInner = self();
                if ( selfInner != null )
                    selfInner.asyncstop();
            }); // ensure all queues are cleaned
        }
    }

    /**
     * synchronous method returning true in case this actor is stopped
     */
    @CallerSideMethod public boolean isStopped() {
        return __stopped;
    }

    /**
     * @return wether this is the "real" implementation or the proxy object
     */
    @CallerSideMethod public boolean isProxy() {
        return getActor() != this;
    }

    /**
     * generic method for untyped messages.
     *
     * @param messageId
     * @return
     */
    public IPromise askMsg(String messageId, Object ... args ) {
        return resolve(null);
    }

    /**
     * generic method for untyped messages.
     *
     * @param messageId
     * @return
     */
    public void tellMsg(String messageId, Object ... args ) { }

    /**
     * generic method for untyped remoting.
     *
     * @param messageId
     * @return
     */
    @CallerSideMethod
    public IPromise ask(String messageId, Object ... args ) {
        boolean isCB = args != null && args.length > 0 && args[args.length-1] instanceof Callback;
        if ( isRemote() && __clientConnection instanceof ConnectionRegistry) {
            Actor sendingActor = Actor.sender.get();
            Object[] newArgs = new Object[args.length+1];
            System.arraycopy(args,0,newArgs,1,args.length);
            newArgs[0] = messageId;
            return (IPromise) getScheduler().enqueueCall((ConnectionRegistry) __clientConnection,sendingActor,getActor(),"ask",newArgs,isCB);
        }
        return (IPromise) getScheduler().enqueueCall(Actor.sender.get(),getActor(),messageId,args,isCB);
    }

    /**
     * generic method for untyped remoting.
     *
     * @param messageId
     * @return
     */
    @CallerSideMethod
    public void tell(String messageId, Object ... args ) {
        boolean isCB = args != null && args.length > 0 && args[args.length-1] instanceof Callback;
        if ( isRemote() && __clientConnection instanceof ConnectionRegistry) {
            Actor sendingActor = Actor.sender.get();
            Object[] newArgs = new Object[args.length+1];
            System.arraycopy(args,0,newArgs,1,args.length);
            newArgs[0] = messageId;
            getScheduler().enqueueCall((ConnectionRegistry) __clientConnection, sendingActor,getActor(),"tell",newArgs,isCB);
            return;
        }
        getScheduler().enqueueCall(Actor.sender.get(),getActor(),messageId,args,isCB);
    }

    /**
     * execute a callable asynchronously (in a different thread) and return a future
     * of the result (delivered in caller thread). Can be used to isolate blocking operations
     *
     * WARNING: do not access local actor state (instance fields) from within the callable (=hidden parallelism).
     * WARNING: a similar named method execute() works different (bad naming)
     *
     * @param callable
     * @param <T>
     * @return
     */
    public <T> IPromise<T> execInThreadPool(Callable<T> callable) {
        Promise<T> prom = new Promise<>();
        __scheduler.runBlockingCall(self(), callable, prom);
        return prom;
    }

    @Local
    public void cyclic(long interval, Callable<Boolean> toRun) {
        if ( ! isStopped() ) {
            Boolean res = true;
            try {
                res = toRun.call();
            } catch (Exception e) {
                Log.Warn(this,e);
            }
            if (res)
                self().delayed(interval,() -> cyclic(interval,toRun) );
        }
    }

    @Local
    public void debounce(long timeout, String tag, Runnable toRun ) {
        if ( _debounceMap == null )
            _debounceMap = new HashMap<>(7);
        _debounceMap.put(tag,toRun);
        delayed(timeout, () -> {
            if ( _debounceMap.get(tag) == toRun ) {
                _debounceMap.remove(tag);
                toRun.run();
            }
        });
    }


    /**
     * schedule an action or call delayed.
     * typical use case:
     * delayed( 100, () -> { self().doAction( x, y,  ); } );
     *
     */
    @CallerSideMethod @Local
    public void delayed( long millis, final Runnable toRun ) {
        __scheduler.delayedCall(millis, inThread(self(), toRun));
    }

    /**
     * WARNING: call rarely, this method might have O(n) runtime with default unbounded queues.
     * @return true if mailbox fill size is ~half capacity
     */
    @CallerSideMethod public boolean isMailboxPressured() {
        return __mailbox.size() * 2 > __mbCapacity;
    }

    @CallerSideMethod public boolean isEmpty() {
        return __mailbox.isEmpty() && __cbQueue.isEmpty();
    }
    /**
     * @return the scheduler associated with this actor (determines scheduling of processing of actors to threads)
     */
    @CallerSideMethod public Scheduler getScheduler() {
        return __scheduler;
    }

    /**
     * WARNING: call rarely, this method might have O(n) runtime with default unbounded queues.
     * @return wether the callback queue is mor than half full (can indicate overload)
     */
    @CallerSideMethod public boolean isCallbackQPressured() {
        return __cbQueue.size() * 2 > __mbCapacity;
    }

    /**
     * WARNING: call rarely, this method might have O(n) runtime with default unbounded queues.
     * @return an estimation on the queued up entries in the mailbox. Can be used for bogus flow control
     */
    @CallerSideMethod public int getMailboxSize() {
        return __mailbox.size();
    }

    /**
     * WARNING: call rarely, this method might have O(n) runtime with default unbounded queues.
     * @return summed queue size of mailbox+callback queue
     */
    @CallerSideMethod public int getQSizes() {
        return getCallbackSize()+getMailboxSize();
    }

    /**
     * WARNING: call rarely, this method might have O(n) runtime with default unbounded queues.
     * @return an estimation on the queued up callback entries. Can be used for bogus flow control.
     */
    @CallerSideMethod public int getCallbackSize() {
        return __cbQueue.size();
    }

    /**
     * wraps an interface into a proxy of that interface. The proxy object automatically enqueues all
     * calls made on the interface onto the callback queue.
     * Can be used incase one needs to pass other callback interfaces then built in Callback object.
     * Stick to using 'Callback' class if possible.
     *
     * @param proxy
     * @param cbInterface
     * @param <T>
     * @return
     */
    @CallerSideMethod
    protected <T> T inThread(Actor proxy, T cbInterface) {
        return __scheduler.inThread(proxy, cbInterface);
    }

    /**
     * Debug method.
     * can be called to check actor code is actually single threaded.
     * By using custom callbacks/other threading bugs accidental multi threading
     * can be detected that way.
     *
     */
    protected final void checkThread() {
        if (getCurrentDispatcher() != null && getCurrentDispatcher() != Thread.currentThread()) {
            Log.Error(this,"UNEXPECTED MULTITHREADING");
            throw new RuntimeException("Wrong Thread");
        } else if ( getCurrentDispatcher() == null ){
            Log.Error(this,"Not in Dispatcher Thread");
            throw new RuntimeException("Not in Dispatcher Thread:"+Thread.currentThread().getName());
        }
    }

    /**
     * @return the proxy of this actor
     */
    @CallerSideMethod public Actor getActorRef() {
        return __self;
    }

    /**
     * @return wether this is a proxy to a remotely running actor
     */
    @CallerSideMethod public boolean isRemote() {
        return __remoteId != 0;
    }

    /**
     * closes associated remote connection(s) if present. NOP otherwise.
     * Close refers to "unmapping" the serving actor, underlying network connections will be
     * closed. All clients get disconnected
     */
    @Local
    public void close() {
        if (__connections != null) {
            final ConcurrentLinkedQueue<ConnectionRegistry> prevCon = getActorRef().__connections;
            getActorRef().__connections = null;
            getActor().__connections = null;
            prevCon.forEach((con) -> con.close(this));
        }
    }

    /**
     * closes the connection to the remote client which has sent currently executing message.
     * If current message was not sent by a client, NOP.
     */
    protected void closeCurrentClient() {
        ConnectionRegistry remoteConnection = connection.get();
        if ( remoteConnection != null ) {
            delayed(1000, () -> remoteConnection.close(this));
        }
    }

    @Local
    public void asyncstop() {
        hasStopped();
        __stop();
    }

    /**
     * override to clean up / stop dependend actors
     */
    protected void hasStopped() {
    }

    /**
     * avoids exception when closing an actor after stop has been called.
     */
    @CallerSideMethod public void stopSafeClose() {
        if ( isStopped() ) {
            getActor().close(); // is threadsafe
        } else {
            self().close();
        }
    }

    /**
     * can be used to wait for all messages having been processed and get a signal from the returned future once this is complete
     * @return
     */
    public IPromise ping() {
        return new Promise<>(0);
    }

    protected TicketMachine __ticketMachine;

    /**
     * enforce serial execution of asynchronous tasks. The 'toRun' closure must call '.complete()' (=empty result) on the given future
     * to signal his processing has finished and the next item locked on 'transactionKey' can be processed.
     *
     * @param transactionKey
     * @param toRun
     */
    protected void serialOn( Object transactionKey, Consumer<IPromise> toRun ) {
        if ( isProxy() )
            throw new RuntimeException("cannot call on actor proxy object");
        if ( __ticketMachine == null ) {
            __ticketMachine = new TicketMachine();
        }
        __ticketMachine.getTicket(transactionKey).onResult(finSig -> {
            try {
                toRun.accept(finSig);
            } catch (Throwable th) {
                Log.Warn(Actor.this,th);
            }
        });
    }

    /**
     * @return true wether this actor is published to network
     */
    @CallerSideMethod public boolean isPublished() {
        return __connections != null && __connections.peek() != null;
    }

    /**
     * just enqueue given runable to this actors mailbox and execute on the actor's thread
     * WARNING: the similar named method execInThreadPool() works different (bad naming)
     * @param command
     */
    @CallerSideMethod @Local @Override
    public void execute(Runnable command) {
        self().__submit(command);
    }

    @CallerSideMethod @Local
    public DispatcherThread getCurrentDispatcher() {
        return (DispatcherThread) __currentDispatcher;
    }

    protected ConcurrentLinkedQueue<ConnectionRegistry> getConnections() {
        return __connections;
    }

    @CallerSideMethod
    public Actor getUntypedRef() {
        Actor actor = new Actor();
        actor.__publishTarget = self();
        return actor;
    }

////////////////////////////// internals ///////////////////////////////////////////////////////////////////

    @CallerSideMethod public void __addStopHandler( Callback<SELF> cb ) {
        if ( __stopHandlers == null ) {
            getActorRef().__stopHandlers = new ConcurrentLinkedQueue();
            getActor().__stopHandlers = getActorRef().__stopHandlers;
        }
        __stopHandlers.add(cb);
    }

    @CallerSideMethod public void __addRemoteConnection( ConnectionRegistry con ) {
        if ( __connections == null ) {
            getActorRef().__connections = new ConcurrentLinkedQueue<ConnectionRegistry>();
            getActor().__connections = getActorRef().__connections;
        }
        if ( ! __connections.contains(con) ) {
            __connections.add(con);
            if ( this instanceof RemotedActor) {
                String connectionIdentifier = con.getSocketRef().getConnectionIdentifier();
                ((RemotedActor) this).hasBeenPublished(connectionIdentifier);
            }
        }
    }

    @CallerSideMethod public void __removeRemoteConnection( ConnectionRegistry con ) {
        if ( __connections != null ) {
            __connections.remove(con);
        }
    }
    @CallerSideMethod public void __stop() { __stopImpl(); }

    // set to stopped and inform all stop listeners
    @CallerSideMethod protected void __stopImpl() {
        Log.Debug(this,"stopping actor "+getClass().getSimpleName());
        Actor self = __self;
        if ( self == null || getActor() == null || (self.isStopped() && getActor().isStopped()) )
            return;
        getActorRef().__stopped = true;
        getActor().__stopped = true;
        if (__stopHandlers!=null) {
            __stopHandlers.forEach( (cb) -> cb.complete(self(), null) );
            __stopHandlers.clear();
        }
        // remove ref to real actor as ref might still be referenced in threadlocals and
        // queues.
        //FIXME: this causes NPE instead of deadletter
//        try {
//            getActorRef().getClass().getField("__target").set( getActorRef(), null );
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        }
        throw InternalActorStoppedException.Instance;
    }

    // dispatch an outgoing call to the target actor queue. Runs in Caller Thread
    @CallerSideMethod public Object __enqueueCall( Actor receiver, String methodName, Object args[], boolean isCB ) {
        if ( __stopped ) {
            if ( methodName.equals("stop") ) // ignore double stop
                return null;
            __addDeadLetter(receiver, methodName);
//            throw new RuntimeException("Actor " + this + " received message after being stopped " + methodName);
        }
        return __scheduler.enqueueCall(sender.get(), receiver, methodName, args, isCB);
    }

    @CallerSideMethod public void __addDeadLetter(Actor receiver, String methodName) {
        String senderString = sender.get() == null ? "null" : sender.get().getClass().getName();
        String s = "DEAD LETTER: sender:" + senderString + " receiver::msg:" + receiver.getClass().getSimpleName() + "::" + methodName;
        s = s.replace("_ActorProxy","");
        Actors.AddDeadLetter(s);
    }

    // FIXME: would be much better to do lookup at method invoke time INSIDE actor thread instead of doing it on callside (contended)
    ConcurrentHashMap methodCache,interceptedCache;
    @CallerSideMethod public Method __getCachedMethod(String methodName, Actor actor, BiFunction<Actor, String, Boolean> callInterceptor) {
        // FIXME: this will fail once an actor is used with different interceptor policies (remoted twice with different policies)
        // assumption: only remote calls can be intercepted, interceptor != null => remote call
        if (callInterceptor != null) {
            if ( interceptedCache == null ) {
                interceptedCache = new ConcurrentHashMap(7);
            }
        } else{
            if ( methodCache == null ) {
                methodCache = new ConcurrentHashMap(7);
            }
        }
        ConcurrentHashMap mcache = callInterceptor != null ? interceptedCache:methodCache;
        Method method = (Method) mcache.get(methodName);
        if ( method == null ) {
            Method[] methods = actor.getActor().getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if ( m.getName().equals(methodName) ) {
                    if ( callInterceptor == null || callInterceptor.apply(actor,methodName) ) {
                        mcache.put(methodName, m);
                        method = m;
                        break;
                    }
                }
            }
        } else if ( callInterceptor != null ){
            if ( !callInterceptor.apply(actor,methodName) ) {
                return null;
            }
        }
        return method;
    }

    /**
     * called if a message invokation from remote is received
     * @return true if a new promise has been created
     */
    @CallerSideMethod public boolean __dispatchRemoteCall(ObjectSocket objSocket, RemoteCallEntry rce, ConnectionRegistry registry, List<IPromise> createdFutures, Object authContext, BiFunction<Actor, String, Boolean> callInterceptor, long delayCode) {
        rce.unpackArgs(registry.getConf());
        try {
            if ( delayCode == RateLimitEntry.REJECT )
                throw new RateLimitException();
            Object future = getScheduler().enqueueCallFromRemote(registry, null, self(), rce.getMethod(), rce.getArgs(), false, null, callInterceptor, rce);
            if ( future instanceof IPromise) {
                Promise p = null;
                if ( createdFutures != null ) {
                    p = new Promise();
                    createdFutures.add(p);
                }
                final Promise finalP = p;
                final RemoteCallEntry finalRce = rce;
                ((IPromise) future).then( (r,e) -> {
                    Runnable runnable = () -> {
                        try {
                            Object finalR = r;
                            if ( registry.isJsonSerialized() && finalRce.getMethodHandle() != null ) {
                                finalR = getScheduler().mapResult(r,finalRce);
                            }
                            registry.receiveCBResult(objSocket, finalRce.getFutureKey(), finalR, e);
                            if (finalP != null)
                                finalP.resolve();
                        } catch (Exception ex) {
                            Log.Warn(this, ex, "--");
                        }
                    };
                    if ( Thread.currentThread() != __currentDispatcher )
                        self().execute(runnable);
                    else
                        runnable.run();
                });
            }
        } catch (Throwable th) {
            if ( th instanceof RateLimitException == false )
                Log.Warn(this,th);
            if ( rce.getFutureKey() != 0 ) {
                    self().execute(() -> {
                        try {
                            registry.receiveCBResult(objSocket, rce.getFutureKey(), null, th instanceof RateLimitException ? ""+th : FSTUtil.toString(th) );
                        } catch (Exception e) {
                            Log.Error(this,e);
                        }
                    });
            } else {
                if ( th instanceof RateLimitException == false )
                    FSTUtil.<RuntimeException>rethrow(th);
            }
        }
        return createdFutures != null && createdFutures.size() > 0;
    }

    @Local
    public void unpublish() {
        if ( __connections != null )
            __connections.forEach( conreg -> conreg.unpublishActor(this));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Routing specials

    public Callback<RemoteCallEntry> zzServerMsgCallback;

    /**
     * cb is invoked once the server sends back messages to a client,
     * the client does not necessary have an actor running but might simply connect
     * using ordinary mt code
     * @param cb
     */
    @Local @CallerSideMethod public void setServerMsgCallback(Callback<RemoteCallEntry> cb) {
        zzServerMsgCallback = cb;
    }
    /**
     * a krouter client should ping using this method.
     *
     * @param tim
     * @param publishedActorIds - published actors of sender
     * @return
     */
    @Remoted
    public IPromise<Long> router$clientPing(long tim, long[] publishedActorIds) {
        ConnectionRegistry remoteConnection = connection.get();
        if ( remoteConnection != null ) {
            remoteConnection.pingFromRoutingClient();
        }
        return resolve(tim);
    }

    private long zzLastLiveActorIds[];
    @Remoted
    public void zzRoutingRefGC(long ids[]) {
        if ( zzRoutingGCEnabled ) {
            ConnectionRegistry connectionRegistry = connection.get();
            if ( zzLastLiveActorIds == null ) {
                zzLastLiveActorIds = connectionRegistry.getPublishedActorIds();
            } else {
                // to overcome timings, do gc against previous set of remoted actors
                // remove not-alive actors. (check each id against current live set)
//                System.out.println("GC reported alive:"+Arrays.toString(ids));
//                System.out.println("GC last seen alive:"+Arrays.toString(zzLastLiveActorIds));
//                System.out.println("GC alive:"+Arrays.toString(connectionRegistry.getPublishedActorIds()));
//                System.out.println("GC alive remoted:"+Arrays.toString(connectionRegistry.getRemotedActorIds()));
                for (int i = 0; i < zzLastLiveActorIds.length; i++) {
                    long zzLastLiveActorId = zzLastLiveActorIds[i];
                    if ( zzLastLiveActorId < 2 )
                        continue;
                    boolean alive = false;
                    for (int j = 0; j < ids.length; j++) {
                        long id = ids[j];
                        if ( id == zzLastLiveActorId ) {
                            alive = true;
                            break;
                        }
                    }
                    if ( ! alive ) {
                        Actor publishedActor = connectionRegistry.getPublishedActor(zzLastLiveActorId);
                        if ( publishedActor != null && ! publishedActor.isStopped() && publishedActor.isPublished() ) {
                            System.out.println("unpublishing " + zzLastLiveActorId + " " + publishedActor);
                            connectionRegistry.unpublishActor(publishedActor);
                            publishedActor.stop();
                        }
                    } else {
//                        System.out.println("alive: "+zzLastLiveActorId);
                    }
                }
                zzLastLiveActorIds = connectionRegistry.getPublishedActorIds();
            }
        }
    }

    @Remoted
    public void zzkrouterLostClient() {
        ConnectionRegistry remoteConnection = connection.get();
        if ( this instanceof ServingActor ) {
            ((ServingActor)this).clientDisconnected(null,null);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Monitoring
    //

    @Override @Local
    public IPromise getReport() {
        return new Promise( new ActorReport(getActor().getClass().getSimpleName(), getMailboxSize(), getCallbackSize() ) );
    }

    @Override @Local
    public IPromise<Monitorable[]> getSubMonitorables() {
        return new Promise<>(new Monitorable[0]);
    }

    // special in order to send actor ref's without referring to class (routing)
    public Actor __publishTarget;

    public static class ActorReport {
        String clz;
        int mailboxSize;
        int cbqSize;

        public ActorReport() {
        }

        public ActorReport(String clz, int mailboxSize, int cbqSize) {
            this.clz = clz;
            this.mailboxSize = mailboxSize;
            this.cbqSize = cbqSize;
        }

        public String getClz() {
            return clz;
        }

        public int getMailboxSize() {
            return mailboxSize;
        }

        public int getCbqSize() {
            return cbqSize;
        }

    }


}

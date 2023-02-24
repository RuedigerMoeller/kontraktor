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

package org.nustaq.kontraktor.impl;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.base.JsonMappable;
import org.nustaq.kontraktor.remoting.base.JsonMapped;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

/**
 * Created by ruedi on 02/04/15.
 */
public class SimpleScheduler implements Scheduler {

    public static final boolean DEBUG_SCHEDULING = true;
    /**
     * time ms until a warning is printed once a sender is blocked by a
     * full actor queue
     */
    public static long BLOCKED_MS_TIL_WARN = 1000;
    public static int DEFQSIZE = 32768; // will be alligned to 2^x

    protected ObjectMapper mapper;

    protected BackOffStrategy backOffStrategy = new BackOffStrategy();
    protected DispatcherThread myThread;
    int qsize = DEFQSIZE;

    protected SimpleScheduler(String dummy) {
    }

    public SimpleScheduler() {
        myThread = new DispatcherThread(this,true);
        myThread.start();
    }

    public SimpleScheduler(boolean keepAlive) {
        myThread = new DispatcherThread(this,!keepAlive);
        myThread.start();
    }

    public SimpleScheduler(int qsize) {
        this.qsize = qsize;
        myThread = new DispatcherThread(this,true);
        myThread.start();
    }

    /**
     * maps a JsonMapped result of a promise
     * @param result
     * @return
     */
    public Object mapResult( Object result, RemoteCallEntry rce ) {
        if ( result instanceof JsonValue)
            return result.toString();
        if ( result instanceof JsonMappable == false && ! rce.getMethodHandle().isAnnotationPresent(JsonMapped.class) )
            return result;
        if ( mapper == null ) {
            mapper = ConnectionRegistry.CreateDefaultObjectMapper.get();
        }
        try {
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            Log.Warn(this,e);
        }
        return result;
    }

    /**
     *
     * @param qsize
     * @param keepAlive - keep thread idle even if no actor is scheduled. Required for assisted scheduling e.g.
     * in servers
     */
    public SimpleScheduler(int qsize, boolean keepAlive ) {
        this(qsize,keepAlive,null);
    }

    public SimpleScheduler(int qsize, boolean keepAlive, String name ) {
        this.qsize = qsize;
        myThread = new DispatcherThread(this,!keepAlive);
        myThread.start();
        if ( name != null ) {
            myThread.setName(name);
        }
    }

    @Override
    public int getDefaultQSize() {
        return qsize;
    }

    @Override
    public void pollDelay(int count) {
        backOffStrategy.kYield(count);
    }

    @Override
    public void put2QueuePolling(Queue q, boolean isCBQ, Object o, Object receiver) {
        int count = 0;
        long sleepStart = 0;
        boolean warningPrinted = false;
        while ( ! q.offer(o) ) {
            pollDelay(count++);
            if ( backOffStrategy.isYielding(count) ) {
                Actor sendingActor = Actor.sender.get();
                if ( receiver instanceof Actor && ((Actor) receiver).__stopped ) {
                    String dl;
                    if ( o instanceof CallEntry) {
                        dl = ((CallEntry) o).getMethod().getName();
                    } else {
                        dl = ""+o;
                    }
                    if ( sendingActor != null )
                        sendingActor.__addDeadLetter((Actor) receiver, dl);
                    throw new StoppedActorTargetedException(dl);
                }
                if ( backOffStrategy.isSleeping(count) ) {
                    if ( sleepStart == 0 ) {
                        sleepStart = System.currentTimeMillis();
                    } else if ( ! warningPrinted && System.currentTimeMillis()-sleepStart > BLOCKED_MS_TIL_WARN) {
                        String receiverString;
                        warningPrinted = true;
                        if (receiver instanceof Actor) {
                            if (q == ((Actor) receiver).__cbQueue) {
                                receiverString = receiver.getClass().getSimpleName() + " callbackQ "+((Actor) receiver).getCallbackSize();
                            } else if (q == ((Actor) receiver).__mailbox) {
                                receiverString = receiver.getClass().getSimpleName() + " mailbox "+((Actor) receiver).getMailboxSize();
                            } else {
                                receiverString = receiver.getClass().getSimpleName() + " unknown queue";
                            }
                        } else {
                            receiverString = "" + receiver;
                        }
                        String sender = "";
                        if (sendingActor != null)
                            sender = ", sender:" + sendingActor.getActor().getClass().getSimpleName();
                        Log.Lg.warn(this, "Warning: Thread " + Thread.currentThread().getName() + " blocked more than "+BLOCKED_MS_TIL_WARN+"ms trying to put message on " + receiverString + sender + " msg:" + o);
                    }
                }
            }
        }
    }

    public IPromise put2QueuePolling(CallEntry e) {
        final IPromise fut;
        if (e.hasFutureResult() && ! (e.getFutureCB() instanceof CallbackWrapper) ) {
            fut = new Promise();
            e.setFutureCB(new CallbackWrapper( e.getSendingActor() ,new Callback() {
                @Override
                public void complete(Object result, Object error) {
                    fut.complete(result, error);
                }
            }));
        } else
            fut = null;
        Actor targetActor = e.getTargetActor();
        put2QueuePolling( e.isCallback() ? targetActor.__cbQueue : targetActor.__mailbox, false, e, targetActor);
        return fut;
    }

    @Override
    public Object enqueueCall(Actor sendingActor, Actor receiver, String methodName, Object[] args, boolean isCB) {
        return enqueueCallFromRemote((ConnectionRegistry) receiver.__clientConnection,sendingActor,receiver,methodName,args,isCB, null, null, null);
    }

    @Override
    public Object enqueueCall(ConnectionRegistry reg, Actor sendingActor, Actor receiver, String methodName, Object[] args, boolean isCB) {
        return enqueueCallFromRemote( reg,sendingActor,receiver,methodName,args,isCB, null, null, null);
    }

    @Override
    public Object enqueueCallFromRemote(ConnectionRegistry reg, Actor sendingActor, Actor receiver, String methodName, Object[] args, boolean isCB, Object securityContext, BiFunction<Actor, String, Boolean> callInterceptor, RemoteCallEntry remoteCallEntry) {
        // System.out.println("dispatch "+methodName+" "+Thread.currentThread());
        // here sender + receiver are known in a ST context
        Actor actor = receiver.getActor();
        Method method = actor.__getCachedMethod(methodName, actor, callInterceptor);
        Parameter[] parameterTypes = null;
        if ( reg != null && reg.isJsonSerialized() && method != null )
            parameterTypes = method.getParameters();

        if ( method == null )
            throw new RemoteMethodNotFoundException("unknown method "+methodName+" on "+actor);
        if ( remoteCallEntry != null )
            remoteCallEntry.setMethodHandle(method);
        // scan for callbacks in arguments ..
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if ( arg instanceof Callback) {
                args[i] = new CallbackWrapper<>(sendingActor,(Callback<Object>) arg);
            }
            // process jsonmapped args (jackson object mapper / eclipse api)
            if ( parameterTypes != null && arg instanceof String && (reg==null || reg.isJsonSerialized()) ) {
                if ( parameterTypes[i].isAnnotationPresent(JsonMapped.class) || JsonMappable.class.isAssignableFrom(parameterTypes[i].getType()))
                {
                    if ( mapper == null )
                        mapper = ConnectionRegistry.CreateDefaultObjectMapper.get();
                    try {
                        args[i] = mapper.readerFor(parameterTypes[i].getType()).readValue((String)arg);
                    } catch (IOException e) {
                        Log.Warn(this,e);
                    }
                } else if ( JsonValue.class.isAssignableFrom(parameterTypes[i].getType()) ) {
                    args[i] = Json.parse((String)arg);
                }
            }
        }

        CallEntry e = createCallentry(reg, args, isCB, actor, method);
        return put2QueuePolling(e);
    }

    protected CallEntry createCallentry(ConnectionRegistry reg, Object[] args, boolean isCB, Actor actor, Method method) {
        CallEntry e = new CallEntry(
                actor, // target
                method,
                args,
                Actor.sender.get(), // enqueuer
                actor,
                isCB
        );
        e.setRemoteRefRegistry(reg);
        return e;
    }

    @Override
    public void terminateIfIdle() {
        myThread.setAutoShutDown(true);
    }

    @Override
    public void threadStopped(DispatcherThread th) {
    }

    public void setKeepAlive(boolean b) {
        if ( myThread != null )
            myThread.setAutoShutDown(!b);
    }

    class CallbackInvokeHandler implements InvocationHandler {

        final Object target;
        final Actor targetActor;

        public CallbackInvokeHandler(Object target, Actor act) {
            this.target = target;
            this.targetActor = act;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ( method.getDeclaringClass() == Object.class )
                return method.invoke(proxy,args); // toString, hashCode etc. invoke sync (DANGER if hashcode accesses mutable local state)
            if ( target != null ) {
                CallEntry ce = new CallEntry(target,method,args, Actor.sender.get(), targetActor, true);
                put2QueuePolling(targetActor.__cbQueue, true, ce, targetActor);
            }
            return null;
        }
    }

    @Override
    public InvocationHandler getInvoker(Actor dispatcher, Object toWrap) {
        return new CallbackInvokeHandler(toWrap, dispatcher);
    }

    public static Class[] getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> classes = getAllInterfacesForClassAsSet(clazz, null);
        Class cl[] = new Class[classes.size()];
        classes.toArray(cl);
        return cl;
    }

    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
        if (clazz.isInterface()) {
            return Collections.<Class<?>>singleton(clazz);
        }
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        while (clazz != null) {
            Class<?>[] ifcs = clazz.getInterfaces();
            for (Class<?> ifc : ifcs) {
                interfaces.addAll(getAllInterfacesForClassAsSet(ifc, classLoader));
            }
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }


    @Override
    public <T> T inThread(Actor actor, T callback) {
        if (actor==null) {
            return callback;
        }
        if ( Proxy.isProxyClass(callback.getClass()) )
            return callback;
        Class<?>[] interfaces = getAllInterfaces(callback.getClass());
        InvocationHandler invoker = actor.__scheduler.getInvoker(actor, callback);
        if ( invoker == null ) // called from outside actor world
        {
            return callback; // callback in callee thread
        }
        return (T) Proxy.newProxyInstance(callback.getClass().getClassLoader(), interfaces, invoker);
    }

    @Override
    public void delayedCall(long millis, Runnable toRun) {
        Actors.delayedCalls.schedule(new TimerTask() {
            @Override
            public void run() {
                toRun.run();
            }
        }, millis);
    }

    public static void DelayedCall(long millis, Runnable toRun) {
        Actors.delayedCalls.schedule(new TimerTask() {
            @Override
            public void run() {
                toRun.run();
            }
        }, millis);
    }

    @Override
    public <T> void runBlockingCall(Actor emitter, Callable<T> toCall, Callback<T> resultHandler) {
        final CallbackWrapper<T> resultWrapper = new CallbackWrapper<>(emitter,resultHandler);
        Actors.exec.execute(() -> {
            try {
                resultWrapper.complete(toCall.call(), null);
            } catch (Throwable th) {
                resultWrapper.complete(null, th);
            }
        });
    }

    @Override
    public DispatcherThread assignDispatcher(int minLoadPerc) {
        return myThread;
    }

    @Override
    public void rebalance(DispatcherThread dispatcherThread) {
    }

    @Override
    public BackOffStrategy getBackoffStrategy() {
        return backOffStrategy;
    }

    @Override
    public void tryStopThread(DispatcherThread dispatcherThread) {

    }

    @Override
    public void tryIsolate(DispatcherThread dp, Actor actorRef) {

    }

    @Override
    public int getNumActors() {
        return myThread.getActorsNoCopy().length;
    }

    @Override
    public IPromise getReport() {
        return new Promise<>(new SchedulingReport(1,getDefaultQSize(),0));
    }

    @Override
    public IPromise<Monitorable[]> getSubMonitorables() {
        return new Promise<>(new Monitorable[] { myThread } );
    }
}

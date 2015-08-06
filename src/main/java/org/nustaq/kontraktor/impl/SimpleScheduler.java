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

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 02/04/15.
 */
public class SimpleScheduler implements Scheduler {

    public static final boolean DEBUG_SCHEDULING = true;
    /**
     * time ms until a warning is printed once a sender is blocked by a
     * full actor queue
     */
    public static long BLOCKED_MS_TIL_WARN = 5000;
    public static int DEFQSIZE = 32768; // will be alligned to 2^x

    protected BackOffStrategy backOffStrategy = new BackOffStrategy();
    protected DispatcherThread myThread;
    int qsize = DEFQSIZE;

    protected SimpleScheduler(String dummy) {
    }

    public SimpleScheduler() {
        myThread = new DispatcherThread(this,true);
        myThread.start();
    }

    public SimpleScheduler(int qsize) {
        this.qsize = qsize;
        myThread = new DispatcherThread(this,true);
        myThread.start();
    }

    /**
     *
     * @param qsize
     * @param keepAlive - keep thread idle even if no actor is scheduled. Required for assisted scheduling e.g.
     * in servers
     */
    public SimpleScheduler(int qsize, boolean keepAlive ) {
        this.qsize = qsize;
        myThread = new DispatcherThread(this,!keepAlive);
        myThread.start();
    }

    @Override
    public int getDefaultQSize() {
        return qsize;
    }

    @Override
    public void pollDelay(int count) {
        backOffStrategy.yield(count);
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
                if ( sendingActor != null && sendingActor.__throwExAtBlock )
                    throw ActorBlockedException.Instance;
                if ( backOffStrategy.isSleeping(count) ) {
                    if ( sleepStart == 0 ) {
                        sleepStart = System.currentTimeMillis();
                    } else if ( ! warningPrinted && System.currentTimeMillis()-sleepStart > BLOCKED_MS_TIL_WARN) {
                        String receiverString;
                        warningPrinted = true;
                        if (receiver instanceof Actor) {
                            if (q == ((Actor) receiver).__cbQueue) {
                                receiverString = receiver.getClass().getSimpleName() + " callbackQ";
                            } else if (q == ((Actor) receiver).__mailbox) {
                                receiverString = receiver.getClass().getSimpleName() + " mailbox";
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
        return enqueueCallFromRemote(null,sendingActor,receiver,methodName,args,isCB);
    }

    @Override
    public Object enqueueCallFromRemote(RemoteRegistry reg, Actor sendingActor, Actor receiver, String methodName, Object[] args, boolean isCB) {
        // System.out.println("dispatch "+methodName+" "+Thread.currentThread());
        // here sender + receiver are known in a ST context
        Actor actor = receiver.getActor();
        Method method = actor.__getCachedMethod(methodName, actor);

        if ( method == null )
            throw new RuntimeException("unknown method "+methodName+" on "+actor);
        // scan for callbacks in arguments ..
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if ( arg instanceof Callback) {
                args[i] = new CallbackWrapper<>(sendingActor,(Callback<Object>) arg);
            }
        }

        CallEntry e = new CallEntry(
                actor, // target
                method,
                args,
                Actor.sender.get(), // enqueuer
                actor,
                isCB
        );
        e.setRemoteRefRegistry(reg);
        return put2QueuePolling(e);
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
            myThread.setAutoShutDown(b);
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

    @Override
    public <T> T inThread(Actor actor, T callback) {
        if (actor==null) {
            return callback;
        }
        Class<?>[] interfaces = callback.getClass().getInterfaces();
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

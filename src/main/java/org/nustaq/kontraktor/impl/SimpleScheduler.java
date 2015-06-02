package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * Created by ruedi on 02/04/15.
 */
public class SimpleScheduler implements Scheduler {

    public static final boolean DEBUG_SCHEDULING = true;
    public static int DEFQSIZE = 32768; // will be alligned to 2^x

    protected BackOffStrategy backOffStrategy = new BackOffStrategy();
    protected DispatcherThread myThread;
    int qsize = DEFQSIZE;

    protected SimpleScheduler(String dummy) {
    }

    public SimpleScheduler() {
        myThread = new DispatcherThread(this,false);
        myThread.start();
    }

    public SimpleScheduler(int qsize) {
        this.qsize = qsize;
        myThread = new DispatcherThread(this,false);
        myThread.start();
    }

    @Override
    public int getMaxThreads() {
        return 1;
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
    public void threadStopped(DispatcherThread th) {
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
    public IPromise getReport() {
        return new Promise<>(new SchedulingReport(1,getDefaultQSize(),0));
    }

    @Override
    public IPromise<Monitorable[]> getSubMonitorables() {
        return new Promise<>(new Monitorable[] { myThread } );
    }
}

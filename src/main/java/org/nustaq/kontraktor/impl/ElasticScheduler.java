package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ruedi on 13.06.14.
 */
public class ElasticScheduler implements Scheduler {

    int maxThread = 8; // Runtime.getRuntime().availableProcessors();
    protected BackOffStrategy backOffStrategy = new BackOffStrategy(); // FIXME: should not be static
    DispatcherThread threads[];

    int defQSize = 30000;
    protected ExecutorService exec = Executors.newCachedThreadPool();
    protected Timer delayedCalls = new Timer();

    public ElasticScheduler(int maxThreads, int defQSize) {
        this.maxThread = maxThreads;
        this.defQSize = defQSize;
        threads = new DispatcherThread[maxThreads];
    }

    @Override
    public int getMaxThreads() {
        return maxThread;
    }

    @Override
    public int getDefaultQSize() {
        return defQSize;
    }

//    @Override
    public Future put2QueuePolling(CallEntry e) {
        final Future fut;
        if (e.hasFutureResult()) {
            fut = new Promise();
            e.setFutureCB(new CallbackWrapper( e.getSendingActor() ,new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    fut.receiveResult(result,error);
                }
            }));
        } else
            fut = null;
        put2QueuePolling(e.getTargetActor().__mailbox, e);
        return fut;
    }

    @Override
    public void yield(int count) {
        backOffStrategy.yield(count);
    }

    @Override
    public void put2QueuePolling(Queue q, Object o) {
        int count = 0;
        while ( ! q.offer(o) ) {
            yield(count++);
        }
    }

    @Override
    public Object dispatchCall(Actor sendingActor, Actor receiver, String methodName, Object args[]) {
        // System.out.println("dispatch "+methodName+" "+Thread.currentThread());
        // here sender + receiver are known in a ST context
        Actor actor = receiver.getActor();
        Method method = actor.__getCachedMethod(methodName, actor);

        int count = 0;
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
                actor
        );
        return put2QueuePolling(e);
    }

    public void threadStopped(DispatcherThread th) {
        synchronized(threads) {
            for (int i = 0; i < threads.length; i++) {
                if (threads[i] == th) {
                    threads[i] = null;
                    return;
                }
            }
        }
        throw new RuntimeException("Oops. Unknown Thread");
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
                return method.invoke(proxy,args); // toString, hashCode etc. invoke sync (danger if hashcode access local state)
            if ( target != null ) {
                CallEntry ce = new CallEntry(target,method,args, Actor.sender.get(), targetActor);
                put2QueuePolling(targetActor.__cbQueue, ce);
            }
            return null;
        }
    }

    @Override
    public InvocationHandler getInvoker(Actor dispatcher, Object toWrap) {
        return new CallbackInvokeHandler(toWrap, dispatcher);
    }

    /**
     * Creates a wrapper on the given object enqueuing all calls to INTERFACE methods of the given object to the given actors's queue.
     * This is used to enable processing of resulting callback's in the callers thread.
     * see also @InThread annotation.
     * @param callback
     * @param <T>
     * @return
     */
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
    public void delayedCall(int millis, final Runnable toRun) {
        delayedCalls.schedule(new TimerTask() {
            @Override
            public void run() {
                toRun.run();
            }
        }, millis);
    }

    @Override
    public <T> void runBlockingCall(Actor emitter, final Callable<T> toCall, Callback<T> resultHandler) {
        final CallbackWrapper<T> resultWrapper = new CallbackWrapper<>(emitter,resultHandler);
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    resultWrapper.receiveResult(toCall.call(), null);
                } catch (Throwable th) {
                    resultWrapper.receiveResult(null, th);
                }
            }
        });
    }

    /**
     * wait for all futures to complete and return an array of fulfilled futures
     *
     * e.g. Yield( f1, f2 ).then( (f,e) -> System.out.println( f[0].getResult() + f[1].getResult() ) );
     * @param futures
     * @return
     */
    @Override
    public Future<Future[]> yield(Future... futures) {
        Promise res = new Promise();
        yield(futures, 0, res);
        return res;
    }

    @Override
    public DispatcherThread assignDispatcher() {
        synchronized (threads) { // FIXME race condition if thread terminates inbetween returning and new assignment
            int minLoad = Integer.MAX_VALUE;
            DispatcherThread minThread = findMinLoadThread(minLoad);
            if ( minThread != null ) {
                return minThread;
            }
            DispatcherThread newThreadIfPossible = createNewThreadIfPossible();
            if ( newThreadIfPossible != null ) {
                newThreadIfPossible.start();
                return newThreadIfPossible;
            }
        }
        throw new RuntimeException("could not assign thread. This is a severe error");
    }

    private DispatcherThread findMinLoadThread(int minLoad) {
        synchronized (threads) {
            DispatcherThread minThread = null;
            for (int i = 0; i < threads.length; i++) {
                DispatcherThread thread = threads[i];
                if (thread != null) {
                    int load = thread.getLoad();
                    if (load < minLoad) {
                        minLoad = load;
                        minThread = thread;
                    }
                }
            }
            return minThread;
        }
    }

    private DispatcherThread createNewThreadIfPossible() {
        synchronized (threads) {
            for (int i = 0; i < threads.length; i++) {
                DispatcherThread thread = threads[i];
                if (thread == null) {
                    DispatcherThread th = new DispatcherThread(this);
                    threads[i] = th;
                    return th;
                }
            }
        }
        return null;
    }

    /** called from inside overloaded thread with load
     * all actors assigned to the calling thread therefore can be safely moved
     * @param dispatcherThread
     */
    @Override
    public void rebalance(DispatcherThread dispatcherThread) {
        int load = dispatcherThread.getLoad();
        DispatcherThread minLoadThread = createNewThreadIfPossible();
        if ( minLoadThread != null ) {
            // split
            dispatcherThread.splitTo(minLoadThread);
            minLoadThread.start();
            return;
        }
        minLoadThread = findMinLoadThread(load / 4);
        if ( minLoadThread == null ) {
            // does not pay off. stay on current
//            System.out.println("no rebalance possible");
            return;
        }
        // move cheapest actor up to half load
        synchronized (dispatcherThread.queueList) {
            long minNanos = Long.MAX_VALUE;
            Actor minActor = null;
            for (int i = 0; i < dispatcherThread.queueList.size(); i++) {
                Actor actor = dispatcherThread.queueList.get(i);
                if (actor.__nanos < minNanos) {
                    minNanos = actor.__nanos;
                    minActor = actor;
                }
            }
            if (minActor != null) {
                //                System.out.println("move "+minActor+" from "+dispatcherThread+" to "+minLoadThread);
                dispatcherThread.removeActor(minActor);
                minLoadThread.addActor(minActor);
                dispatcherThread.applyQueueList();
            }
        }
    }

    private void yield(final Future futures[], final int index, final Future result) {
        if ( index < futures.length ) {
            futures[index].then(new Callback() {
                @Override
                public void receiveResult(Object res, Object error) {
                    yield(futures, index + 1, result);
                }
            });
        } else {
            result.receiveResult(futures, null);
        }
    }

}

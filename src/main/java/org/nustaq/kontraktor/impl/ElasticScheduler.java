package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.util.Log;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 13.06.14.
 *
 * A scheduler implementing "vertical" scaling. Instead of distributing load amongs
 * a given set of threads, it increases the number of threads/cores with load.
 * This way an actor has an dedicated thread executing it instead of random thread hopping
 * when using an executor to schedule actor messages.
 * Additionally this way I can use spin locks without going to 800% CPU if threadmax = 8.
 */
public class ElasticScheduler implements Scheduler, Monitorable {

    public static final int MAX_STACK_ON_SYNC_CBDISPATCH = 100000;
    public static int DEFQSIZE = 32768; // will be alligned to 2^x

    public static boolean DEBUG_SCHEDULING = false;
    public static boolean REALLY_DEBUG_SCHEDULING = false; // logs any move and remove

    public static int RECURSE_ON_BLOCK_THRESHOLD = 2;

    int maxThread = Runtime.getRuntime().availableProcessors();
    protected BackOffStrategy backOffStrategy = new BackOffStrategy();
    final DispatcherThread threads[];

    int defQSize = DEFQSIZE;
    protected ExecutorService exec = Executors.newCachedThreadPool();
    public static Timer delayedCalls = new Timer();
    private AtomicInteger isolateCount = new AtomicInteger(0);

    public ElasticScheduler(int maxThreads) {
        this(maxThreads, DEFQSIZE);
    }

    public ElasticScheduler(int maxThreads, int defQSize) {
        this.maxThread = maxThreads;
        this.defQSize = defQSize;
        if ( defQSize <= 1 )
            this.defQSize = DEFQSIZE;
        threads = new DispatcherThread[maxThreads];
    }

    public int getActiveThreads() {
        int res = 0;
        for (int i = 0; i < threads.length; i++) {
            if ( threads[i] != null ) {
                res++;
            }
        }
        return res;
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
        if (e.hasFutureResult() && ! (e.getFutureCB() instanceof CallbackWrapper) ) {
            fut = new Promise();
            e.setFutureCB(new CallbackWrapper( e.getSendingActor() ,new Callback() {
                @Override
                public void settle(Object result, Object error) {
                    fut.settle(result, error);
                }
            }));
        } else
            fut = null;
        Actor targetActor = e.getTargetActor();
        put2QueuePolling( e.isCallback() ? targetActor.__cbQueue : targetActor.__mailbox, false, e, targetActor);
        return fut;
    }

    @Override
    public void pollDelay(int count) {
        backOffStrategy.yield(count);
    }

    @Override
    public void put2QueuePolling(Queue q, boolean isCBQ, Object o, Object receiver) {
        int count = 0;
        boolean warningPrinted = false;
        while ( ! q.offer(o) ) {
            pollDelay(count++);
            if ( count > RECURSE_ON_BLOCK_THRESHOLD && isCBQ) {
                // thread is blocked, try to schedule other actors on this dispatcher (only calbacks+futures!)
                if (Thread.currentThread() instanceof DispatcherThread) {

                    // fixme: think about consequences in depth.
	                //

	                // if blocked trying to put a callback onto a callback queue:
	                // check wether receiving actor is also scheduled on current thread
	                // if so, poll its message queue. fixme: what happens if sender == receiver

//                    Actor sendingActor = Actor.sender.get();
                    DispatcherThread dp = (DispatcherThread) Thread.currentThread();
                    if ( dp.__stack.size() < MAX_STACK_ON_SYNC_CBDISPATCH && dp.getActorsNoCopy().length > 1 ) {
                        Actor recAct = (Actor) receiver;
                        recAct = recAct.getActorRef();
                        if ( dp.schedules(recAct) ) {
                            dp.__stack.add(null);
                            if (dp.pollQs(new Actor[] {recAct})) {
                                count = 0;
                            }
                            dp.__stack.remove(dp.__stack.size()-1);
                        }
                    } else {
//                        System.out.println("max stack depth");
                    }
                }
            }
            if ( backOffStrategy.isYielding(count) ) {
                Actor sendingActor = Actor.sender.get();
                if ( receiver instanceof Actor && ((Actor) receiver).__stopped ) {
                    String dl;
                    if ( o instanceof CallEntry) {
                        dl = ((CallEntry) o).getMethod().getName();
                    } else {
                        dl = ""+o;
                    }
                    sendingActor.__addDeadLetter((Actor) receiver, dl);
                    throw new StoppedActorTargetedException(dl);
                }
                if ( sendingActor != null && sendingActor.__throwExAtBlock )
                    throw ActorBlockedException.Instance;
                if ( backOffStrategy.isSleeping(count) ) {
                    if (!warningPrinted) {
                        warningPrinted = true;
                        String receiverString;
                        if (receiver instanceof Actor) {
                            if (q == ((Actor) receiver).__cbQueue) {
                                receiverString = receiver.getClass().getSimpleName() + " callbackQ";
                            } else if (q == ((Actor) receiver).__mailbox) {
                                receiverString = receiver.getClass().getSimpleName() + " mailbox";
                            } else {
                                receiverString = receiver.getClass().getSimpleName() + " unknown queue";
                            }
                        } else
                            receiverString = "" + receiver;
                        String sender = "";
                        if (sendingActor != null)
                            sender = ", sender:" + sendingActor.getActor().getClass().getSimpleName();
                        if (DEBUG_SCHEDULING)
                            Log.Lg.warn(this, "Warning: Thread " + Thread.currentThread().getName() + " blocked trying to put message on " + receiverString + sender + " msg:" + o);
                    }
                    // decouple current thread
                    if (sendingActor != null && Thread.currentThread() instanceof DispatcherThread) {
                        DispatcherThread dp = (DispatcherThread) Thread.currentThread();
                        dp.schedulePendingAdds();
//                    if ( dp.getActors().length > 1 && dp.schedules( receiver ) )
                        if (dp.getActors().length > 1) // try isolating in any case
                        {
                            if (DEBUG_SCHEDULING)
                                Log.Lg.warn(this, "  try unblock Thread " + Thread.currentThread().getName() + " actors:" + dp.getActors().length);
                            dp.getScheduler().tryIsolate(dp, sendingActor.getActorRef());
                            if (DEBUG_SCHEDULING)
                                Log.Lg.warn(this, "  unblock done Thread " + Thread.currentThread().getName() + " actors:" + dp.getActors().length);
                        } else {
                            if (dp.getActors().length > 1) {
                                // this indicates there are at least two actors on different threads blocking each other
                                // only solution to unlock is increase the Q of one of the actors
//                            System.out.println("POK "+dp.schedules( receiver )+" "+sendingActor.__currentDispatcher+" "+ ((Actor) receiver).__currentDispatcher);
                            }
                        }
                    }
                }
            }
        }
        if ( warningPrinted && DEBUG_SCHEDULING) {
            Log.Lg.warn(this,"Thread "+Thread.currentThread().getName()+" continued");
        }
    }

    @Override
    public Object enqueueCall(Actor sendingActor, Actor receiver, String methodName, Object args[], boolean isCB) {
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
                actor,
                isCB
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
        if ( th.isIsolated() ) {
            if (DEBUG_SCHEDULING)
                Log.Info(this, "  was decoupled one.");
            isolateCount.decrementAndGet();
        }
//        throw new RuntimeException("Oops. Unknown Thread");
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
                return method.invoke(proxy,args); // toString, hashCode etc. invoke sync (DANGER if hashcode accesses local state)
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
    public void delayedCall(long millis, final Runnable toRun) {
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
        exec.execute(() -> {
            try {
                resultWrapper.settle(toCall.call(), null);
            } catch (Throwable th) {
                resultWrapper.settle(null, th);
            }
        });
    }

    /**
     * if a low load thread is avaiable, return it. else try creation of new thread.
     * if this is not possible return thread with lowest load
     * @return
     */
    @Override
    public DispatcherThread assignDispatcher(int minLoadPerc) {
        synchronized (balanceLock) {
            DispatcherThread minThread = findMinLoadThread(minLoadPerc, null);
            if ( minThread != null ) {
                return minThread;
            }
            DispatcherThread newThreadIfPossible = createNewThreadIfPossible();
            if ( newThreadIfPossible != null ) {
                newThreadIfPossible.start();
                return newThreadIfPossible;
            } else {
                return findMinLoadThread(Integer.MIN_VALUE, null); // return thread with lowest load
            }
        }
    }

    private DispatcherThread findMinLoadThread(int minLoad, DispatcherThread dispatcherThread) {
        synchronized (balanceLock) {
            DispatcherThread minThread = null;
            for (int i = 0; i < threads.length; i++) {
                DispatcherThread thread = threads[i];
                if (thread != null && thread != dispatcherThread) {
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
        for (int i = 0; i < threads.length; i++) {
            DispatcherThread thread = threads[i];
            if (thread == null) {
                DispatcherThread th = createDispatcherThread();
                threads[i] = th;
                return th;
            }
        }
        return null;
    }

    /**
     * @return an UNSTARTED dispatcher thread
     */
    protected DispatcherThread createDispatcherThread() {
        return new DispatcherThread(this);
    }

    final Object balanceLock = new Object();

    /** called from inside overloaded thread.
     * all actors assigned to the calling thread therefore can be safely moved
     * @param dispatcherThread
     */
    @Override
    public void rebalance(DispatcherThread dispatcherThread) {
        synchronized (balanceLock) {
            DispatcherThread minLoadThread = assignDispatcher(dispatcherThread.getLoad());
            if (minLoadThread == null || minLoadThread == dispatcherThread) {
                return;
            }
            int qSizes = dispatcherThread.getAccumulatedQSizes();
            // move actors
            Actor[] qList = dispatcherThread.getActors();
            long otherQSizes = minLoadThread.getAccumulatedQSizes();
            if (4*otherQSizes/3>qSizes) {
                if (REALLY_DEBUG_SCHEDULING) {
                    Log.Info(this, "no payoff, skip rebalance load:"+qSizes+" other:"+otherQSizes);
                }
                return;
            }
            for (int i = 0; i < qList.length; i++) {
                Actor actor = qList[i];
                if (otherQSizes + actor.getQSizes() < qSizes - actor.getQSizes()) {
                    otherQSizes += actor.getQSizes();
                    qSizes -= actor.getQSizes();
                    if (REALLY_DEBUG_SCHEDULING)
                        Log.Info(this,"move " + actor.getQSizes() + " myload " + qSizes + " otherload " + otherQSizes + " from "+dispatcherThread.getName()+" to "+minLoadThread.getName() );
                    dispatcherThread.removeActorImmediate(actor);
                    minLoadThread.addActor(actor);
                }
            }
            if ( ! minLoadThread.isAlive() )
                minLoadThread.start();
        }
    }

    // fixme: use currentthread if this is a precondition anyway
    public void tryIsolate(DispatcherThread dispatcherThread, Actor refToExclude /*implicitely indicates unblock*/) {
        if ( dispatcherThread != Thread.currentThread() )
            throw new RuntimeException("bad error");
        synchronized (balanceLock) {
            // move to actor with minimal load
            if ( refToExclude == null ) {
                throw new IllegalArgumentException("excluderef should not be null");
            }
            Actor qList[] = dispatcherThread.getActors();
            DispatcherThread minLoadThread = findMinLoadThread(Integer.MAX_VALUE, dispatcherThread);
            for (int i = 0; i < threads.length; i++) { // avoid further dispatch
                if ( threads[i] == dispatcherThread ) {
                    threads[i] = createDispatcherThread();
                    dispatcherThread.setName(dispatcherThread.getName()+" (isolated)");
                    dispatcherThread.setIsolated(true);
                    isolateCount.incrementAndGet();
                    minLoadThread = threads[i];
                    minLoadThread.start();
                    if (DEBUG_SCHEDULING)
                        Log.Info(this,"created new thread to unblock "+dispatcherThread.getName());
                }
            }
            if ( minLoadThread == null ) {
                // calling thread is already isolate
                // so no creation happened and no minloadthread was found
                minLoadThread = createDispatcherThread();
                minLoadThread.setName(dispatcherThread.getName()+" (isolated)");
                minLoadThread.setIsolated(true);
                isolateCount.incrementAndGet();
                if (DEBUG_SCHEDULING)
                    Log.Info(this,"created new thread to unblock already isolated "+dispatcherThread.getName());
            }
            for (int i = 0; i < qList.length; i++)
            {
                Actor actor = qList[i];
                // sanity, remove me later
                if ( actor.getActorRef() != actor )
                    throw new RuntimeException("this should not happen ever");
                if ( refToExclude != null && refToExclude.getActorRef() != refToExclude ) {
                    throw new RuntimeException("this also");
                }
                if ( actor != refToExclude ) {
                    dispatcherThread.removeActorImmediate(actor);
                    minLoadThread.addActor(actor);
                }
                if (REALLY_DEBUG_SCHEDULING)
                    Log.Info(this,"move for unblock " + actor.getQSizes() + " myload " + dispatcherThread.getAccumulatedQSizes() + " actors " + qList.length);
            }
        }
    }

    /**
     * stepwise move actors onto other dispatchers. Note actual termination is not done here.
     * removes given dispatcher from the scheduling array, so this thread won't be visible to scheduling
     * anymore. In extreme this could lead to high thread numbers, however this behaviour was never observed
     * until now ..
     *
     * FIXME: in case decoupled threads live forever, do a hard stop on them
     * FIXME: sort by load and spread load amongst all threads (current find min and put removedActors on it).
     * @param dispatcherThread
     */
    public void tryStopThread(DispatcherThread dispatcherThread) {
        if ( dispatcherThread != Thread.currentThread() )
            throw new RuntimeException("bad one");
        synchronized (balanceLock) {
            DispatcherThread minLoadThread = findMinLoadThread(Integer.MAX_VALUE, dispatcherThread);
            if (minLoadThread == null)
                return;
            // move to actor with minimal load
            Actor qList[] = dispatcherThread.getActors();
            for (int i = 0; i < threads.length; i++) { // avoid further dispatch
                if ( threads[i] == dispatcherThread ) {
                    threads[i] = null;
                }
            }
            int maxActors2Remove = Math.min(qList.length, qList.length / 5 + 1); // do several steps to get better spread
            for (int i = 0; i < maxActors2Remove; i++)
            {
                Actor actor = qList[i];
                // sanity, remove me later
                if ( actor.getActorRef() != actor )
                    throw new RuntimeException("this should not happen ever");
                dispatcherThread.removeActorImmediate(actor);
                minLoadThread.addActor(actor);
                if (REALLY_DEBUG_SCHEDULING)
                    Log.Info(this,"move for idle " + actor.getQSizes() + " myload " + dispatcherThread.getAccumulatedQSizes() + " actors " + qList.length);
            }
        }
    }

    @Override
    public BackOffStrategy getBackoffStrategy() {
        return backOffStrategy;
    }

    ///////////////////////////////////////////////////////////////////////////
    // monitorable

    @Override
    public Future $getReport() {
        int count = 0;
        for (int i = 0; i < threads.length; i++) {
            if ( threads[i] != null ) {
                count++;
            }
        }
        return new Promise<>(new SchedulingReport(count,defQSize,isolateCount.get()));
    }

    @Override
    public Future<Monitorable[]> $getSubMonitorables() {
        DispatcherThread[] current = threads;
        int count = 0;
        for (int i = 0; i < current.length; i++) {
            if ( current[i] != null )
                count++;
        }
        Monitorable res[] = new Monitorable[count];
        count = 0;
        for (int i = 0; i < current.length; i++) {
            if ( current[i] != null )
                res[count++] = current[i];
        }
        return new Promise<>(res);
    }

    public static class SchedulingReport implements Serializable {

        int numDispatchers;
        int defQSize;
        int isolatedThreads;

        public SchedulingReport() {
        }

        public SchedulingReport(int numDispatchers, int defQSize, int isolatedThreads) {
            this.numDispatchers = numDispatchers;
            this.defQSize = defQSize;
            this.isolatedThreads = isolatedThreads;
        }

        public int getNumDispatchers() {
            return numDispatchers;
        }

        public int getDefQSize() {
            return defQSize;
        }
    }

}

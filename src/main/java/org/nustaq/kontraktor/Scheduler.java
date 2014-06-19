package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.DispatcherThread;

import java.lang.reflect.InvocationHandler;
import java.util.Queue;
import java.util.concurrent.Callable;

/**
 * Created by ruedi on 14.06.14.
 */
public interface Scheduler {

    int getMaxThreads();

    int getDefaultQSize();

    void yield(int count);

    void put2QueuePolling(Queue q, Object o);

    Object dispatchCall(Actor sendingActor, Actor receiver, String methodName, Object args[]);

    void threadStopped(DispatcherThread th);

    InvocationHandler getInvoker(Actor dispatcher, Object toWrap);

    /**
     * Creates a wrapper on the given object enqueuing all calls to INTERFACE methods of the given object to the given actors's queue.
     * This is used to enable processing of resulting callback's in the callers thread.
     * see also @InThread annotation.
     * @param callback
     * @param <T>
     * @return
     */
    <T> T inThread(Actor actor, T callback);

    void delayedCall(int millis, Runnable toRun);

    <T> void runBlockingCall(Actor emitter, Callable<T> toCall, Callback<T> resultHandler);

    /**
     * wait for all futures to complete and return an array of fulfilled futures
     *
     * e.g. Yield( f1, f2 ).then( (f,e) -> System.out.println( f[0].getResult() + f[1].getResult() ) );
     * @param futures
     * @return
     */
    Future<Future[]> yield(Future... futures);

    public DispatcherThread assignDispatcher();

    /** called from inside overloaded thread with load
     * all actors assigned to the calling thread therefore can be safely moved
     * @param dispatcherThread
     */
    void rebalance(DispatcherThread dispatcherThread);
}

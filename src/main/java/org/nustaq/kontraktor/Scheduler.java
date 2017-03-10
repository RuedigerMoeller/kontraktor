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

import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;

import java.lang.reflect.InvocationHandler;
import java.util.Queue;
import java.util.concurrent.Callable;

/**
 * Scheduler manages scheduling of actors to threads. As kontraktor 3.0 simplyfies
 * scheduling compared to 2.0, this class currently doesn't do a lot.
 */
public interface Scheduler extends Monitorable{

    int getDefaultQSize();

    // yield during polling/spinlooping
    void pollDelay(int count);

    void put2QueuePolling(Queue q, boolean isCBQ, Object o, Object sender);

    Object enqueueCall(Actor sendingActor, Actor receiver, String methodName, Object args[], boolean isCB);

    Object enqueueCall(RemoteRegistry reg, Actor sendingActor, Actor receiver, String methodName, Object[] args, boolean isCB);

    Object enqueueCallFromRemote(RemoteRegistry reg, Actor sendingActor, Actor receiver, String methodName, Object args[], boolean isCB, Object securityContext);

    void threadStopped(DispatcherThread th);

    void terminateIfIdle();

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

    void delayedCall(long millis, Runnable toRun);

    <T> void runBlockingCall(Actor emitter, Callable<T> toCall, Callback<T> resultHandler);

    public DispatcherThread assignDispatcher(int minLoadPerc);

    /** called from inside overloaded thread with load
     * all actors assigned to the calling thread therefore can be safely moved
     * @param dispatcherThread
     */
    void rebalance(DispatcherThread dispatcherThread);

    BackOffStrategy getBackoffStrategy();

    void tryStopThread(DispatcherThread dispatcherThread);

    void tryIsolate(DispatcherThread dp, Actor actorRef);

    /**
     * @return number of actors scheduled by this scheduler. Note this
     * is not precise as not thread safe'd.
     */
    int getNumActors();


}

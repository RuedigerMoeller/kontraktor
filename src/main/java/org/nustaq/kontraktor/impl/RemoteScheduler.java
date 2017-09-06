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

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Created by ruedi on 08.08.14.
 *
 * pure dummy buffering messages, which are then polled by a remote forwarder (ConnectionRegistry, RemoteRefPolling etc)
 */
public class RemoteScheduler extends SimpleScheduler {

    public RemoteScheduler() {
        this(DEFQSIZE);
    }

    public RemoteScheduler(int defQSize) {
        super("dummy");
        this.qsize = defQSize;
        myThread = new DispatcherThread(this) {
            @Override
            public synchronized void start() {
                // fake thread, just don't start
            }

            public String toString() {
                return "REMOTEREF "+super.toString();
            }
        };
    }

    // fixme: might execute on remoteref poller ?

    @Override
    public void delayedCall(long millis, Runnable toRun) {
        throw new RuntimeException("cannot be used on a remote reference (no thread)");
    }

    @Override
    public <T> void runBlockingCall(Actor emitter, Callable<T> toCall, Callback<T> resultHandler) {
        throw new RuntimeException("cannot be used on a remote reference (no thread)");
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

}

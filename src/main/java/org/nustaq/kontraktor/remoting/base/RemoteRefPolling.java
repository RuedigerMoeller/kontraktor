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

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * polls queues of remote actor proxies and serializes messages to their associated object sockets.
 *
 * Note for transparent websocket/longpoll reconnect:
 * Terminated / Disconnected remote actors (registries) are removed from the entry list,
 * so regular actor messages sent to a terminated remote actor queue up in its mailbox.
 * Callbacks/Future results from exported callbacks/futures still reach the object socket
 * as these are redirected directly inside serializers. Those queue up in the webobjectsocket's list,
 * as flush is not called anymore because of removement from SendLoop list.
 *
 * In case of TCP remoting, an ActorStopped Exception is thrown if an attempt is made to send a message to a
 * disconnected remote actor.
 *
 * in short: regular messages to disconected remote actors queue up in mailbox, callbacks in object socket buffer
 *
 * For typical client/server alike use cases, there are never remote references as most client api's do
 * not support handing out client remoterefs to servers (javascript, kontraktor bare)
 *
 */
public class RemoteRefPolling implements Runnable {

    ArrayList<ScheduleEntry> sendJobs = new ArrayList<>();

    AtomicInteger instanceCount = new AtomicInteger(0);
    public RemoteRefPolling() {
        instanceCount.incrementAndGet();
    }

    /**
     * return a future which is completed upon connection close
     *
     * @param reg
     *
     * @return future completed upon termination of scheduling (disconnect)
     *
     */
    public IPromise scheduleSendLoop(RemoteRegistry reg) {
        Promise promise = new Promise();
        sendJobs.add(new ScheduleEntry(reg, promise));
        synchronized (this) {
            if ( ! loopStarted ) {
                loopStarted = true;
                Actor.current().execute(this);
            }
        }
        return promise;
    }

    boolean loopStarted = false;
    boolean underway = false;
    Thread pollThread;

    int remoteRefCounter = 0; // counts active remote refs, if none backoff remoteref polling massively eats cpu
    public void run() {
        pollThread = Thread.currentThread();
        if ( underway )
            return;
        underway = true;
        try {
            int count = 1;
            while( count > 0 ) { // as long there are messages, keep sending them
                count = onePoll();
                if ( sendJobs.size() > 0 ) {
                    if ( count > 0 ) {
                        int debug =1;
                    }
                    else {
                        if ( remoteRefCounter == 0 ) // no remote actors registered
                        {
                            Actor.current().delayed(100, this); // backoff massively
                        } else {
                            Actor.current().delayed(1, this); // backoff a bit (remoteactors present, no messages)
                        }
                    }
                } else {
                    // no schedule entries (== no clients)
                    Actor.current().delayed(100, this );
                }
            }
        } finally {
            underway = false;
        }
    }

    protected int onePoll() {
        int count = 1;
        int maxit = 1;
        remoteRefCounter = 0;
        //while ( maxit > 0 && count > 0)
        {
            count = 0;
            for (int i = 0; i < sendJobs.size(); i++) {
                ScheduleEntry entry = sendJobs.get(i);
                if ( entry.reg.getRemoteActorSize() > 0 ) {
                    remoteRefCounter++;
                }
                if ( entry.reg.isTerminated() ) {
                    terminateEntry(i, entry, "terminated", null );
                    i--;
                    continue;
                }
                try {
                    if (entry.reg.pollAndSend2Remote(entry.reg.getWriteObjectSocket())) {
                        count++;
                    }
                } catch (Throwable e) {
                    if ( e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() != null )
                        e = ((InvocationTargetException) e).getTargetException();
                    Log.Error(this,e);
                    if ( e instanceof IOException || e instanceof IOError ) {
                        terminateEntry(i, entry, null, e);
                        i--;
                    }
                }
            }
            maxit--;
        }

        return count;
    }

    protected void terminateEntry(int i, ScheduleEntry entry, Object res, Throwable e) {
        entry.reg.stopRemoteRefs();
        sendJobs.remove(i);
        entry.promise.complete(res,e);
    }

    public static class ScheduleEntry {
        public ScheduleEntry( RemoteRegistry reg, Promise promise) {
            this.reg = reg;
            this.promise = promise;
        }

        RemoteRegistry reg;
        IPromise promise;
    }
}

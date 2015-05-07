package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.RemoteRegistry;
import org.nustaq.kontraktor.util.Log;

import java.util.ArrayList;

/**
 * polls queues of remote actor proxies and serializes messages to their associated object sockets.
 *
 * Terminated / Disconnected remote actors (registries) are removed from the entry list,
 * so regular actor messages sent to a terminated remote actor queue up in its mailbox.
 * Callbacks/Future results from exported callbacks/futures still reach the object socket
 * as these are redirected directly inside serializers. Those queue up in the webobjectsocket's list,
 * as flush is not called anymore because of removement from SendLoop list.
 *
 * in short: regular messages to disconected remote actors queue up in mailbox, callbacks in object socket buffer
 *
 */
public class SenderLoop implements Runnable {

    ArrayList<ActorServerAdapter.ScheduleEntry> sendJobs = new ArrayList<>();
    BackOffStrategy backOffStrategy = new BackOffStrategy().setNanosToPark(1000*1000*2);

    public SenderLoop() {
        new Thread(this, "ActorServer.senderloop").start();
    }

    public IPromise scheduleSendLoop(RemoteRegistry reg) {
        synchronized (sendJobs) {
            Promise promise = new Promise();
            sendJobs.add(new ActorServerAdapter.ScheduleEntry(reg, promise));
            return promise;
        }
    }

    @Override
    public void run() {
        int count = 0;
        while( true ) {
            try {
            synchronized (sendJobs) {
                for (int i = 0; i < sendJobs.size(); i++) {
                    ActorServerAdapter.ScheduleEntry entry = sendJobs.get(i);
                    if ( entry.reg.isTerminated() ) {
                        terminateEntry(i, entry, "terminated", null );
                        i--;
                        continue;
                    }
                    try {
                        if (entry.reg.pollAndSend2Remote(entry.reg.getWriteObjectSocket().get())) {
                            count = 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        terminateEntry(i, entry, null, e);
                        i--;
                    }
                }
            }
            backOffStrategy.yield(count++);
            } catch (Throwable t) {
                Log.Warn(this, t);
            }
        }
    }

    protected void terminateEntry(int i, ActorServerAdapter.ScheduleEntry entry, Object res, Exception e) {
        entry.reg.stopRemoteRefs();
        sendJobs.remove(i);
        entry.promise.complete(res,e);
    }

}

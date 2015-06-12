package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.util.ArrayList;

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
 * This is a rewrite designed to run inside the facade actor's thread instead of scheduling a separate thread.
 *
 */
public class RemoteRefPolling implements Runnable {

    ArrayList<ScheduleEntry> sendJobs = new ArrayList<>();

    public RemoteRefPolling() {
    }

    /**
     * return a future which is completed upon connection close
     *
     * @param reg
     *
     * @return future completed upon termination of scheduling
     *
     */
    public IPromise scheduleSendLoop(RemoteRegistry reg) {
        Promise promise = new Promise();
        sendJobs.add(new ScheduleEntry(reg, promise));
        if ( sendJobs.size() == 1 ) {
            Actor.current().execute(this);
        }
        return promise;
    }

    public void run() {
        Actor.current(); // fail fast
        int count = 1;
        int maxit = 1;
        while ( maxit > 0 && count > 0) {
            count = 0;
            for (int i = 0; i < sendJobs.size(); i++) {
                ScheduleEntry entry = sendJobs.get(i);
                if ( entry.reg.isTerminated() ) {
                    terminateEntry(i, entry, "terminated", null );
                    i--;
                    continue;
                }
                try {
                    if (entry.reg.pollAndSend2Remote(entry.reg.getWriteObjectSocket().get())) {
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    terminateEntry(i, entry, null, e);
                    i--;
                }
            }
            maxit--;
        }
        if ( sendJobs.size() > 0 ) {
            if (count == 0)
                Actor.current().delayed(1, this);
            else
                Actor.current().execute(this);
        }
    }

    protected void terminateEntry(int i, ScheduleEntry entry, Object res, Exception e) {
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

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.util.ArrayList;
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
 * This is a rewrite designed to run inside the facade actor's thread instead of scheduling a separate thread.
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
     * @return future completed upon termination of scheduling
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
    static volatile long lastReport = System.currentTimeMillis();
    static AtomicInteger scansPersec = new AtomicInteger(0);
    public void run() {
        if ( underway )
            return;
        underway = true;
        try {
            boolean pressured = Actor.current().isMailboxPressured() || Actor.current().isCallbackQPressured();
            long nanos = System.nanoTime();
            if ( pressured ) {
                System.out.println("PRESSURE");
            }
            int count = 1;
            int maxit = 1;
            //while ( maxit > 0 && count > 0)
            {
                count = 0;
                scansPersec.incrementAndGet();
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

            long dur = System.nanoTime() - nanos;
            dur /= 1000;

            if ( System.currentTimeMillis() - lastReport > 1000 ) {
                System.out.println("scan duration "+dur+" micros "+scansPersec.get()+" scans/sec, instances:"+instanceCount.get());
                lastReport = System.currentTimeMillis();
                scansPersec.set(0);
            }

            for (int i = 0; i < toCompleteAfterRun.size(); i++) {
                IPromise iPromise = toCompleteAfterRun.get(i);
                iPromise.complete();
            }
            toCompleteAfterRun.clear();

            if ( sendJobs.size() > 0 ) {
                if ( count > 0 )
                    Actor.current().delayed(1, this);
                else
                    Actor.current().delayed(3, this);
            } else {
                Actor.current().delayed(10, this );
            }
        } finally {
            underway = false;
        }
    }

    protected void terminateEntry(int i, ScheduleEntry entry, Object res, Exception e) {
        entry.reg.stopRemoteRefs();
        sendJobs.remove(i);
        entry.promise.complete(res,e);
    }

    ArrayList<IPromise> toCompleteAfterRun = new ArrayList<>();
    public void completeAfterQPoll(Promise res) {
        toCompleteAfterRun.add(res);
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

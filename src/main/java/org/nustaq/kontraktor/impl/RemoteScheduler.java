package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;

import java.util.concurrent.Callable;

/**
 * Created by ruedi on 08.08.14.
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
}

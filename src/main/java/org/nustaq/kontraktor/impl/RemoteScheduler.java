package org.nustaq.kontraktor.impl;

/**
 * Created by ruedi on 08.08.14.
 */
public class RemoteScheduler extends ElasticScheduler {

    public RemoteScheduler() {
        super(1);
    }

    public RemoteScheduler(int defQSize) {
        super(1, defQSize);
    }

    @Override
    protected DispatcherThread createDispatcherThread() {
        return new DispatcherThread(this) {
            @Override
            public synchronized void start() {
                // fake thread, just don't start
            }
        };
    }

}

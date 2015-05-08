package org.nustaq.kontraktor.impl;

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

}

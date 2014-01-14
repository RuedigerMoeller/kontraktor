package de.ruedigermoeller.abstraktor.impl;

import de.ruedigermoeller.abstraktor.ActorScheduler;
import de.ruedigermoeller.abstraktor.Dispatcher;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 1/8/14.
 */
public class DefaultScheduler implements ActorScheduler {

    static class WorkerStats {
        int lastQSize;
        int curQSize;
        long lastTime;
        long curTime;
        int growCount;
        int nextQSize;

        public void update( DefaultDispatcher disp) {
            disp.calcQSize();
            lastQSize = curQSize;
            curQSize = disp.getQueueSize();
            lastTime = curTime;
            curTime = System.nanoTime();
            if ( curQSize-lastQSize > 0 ) {
                growCount++;
            } else
                growCount = 0;
        }

        public long getIntervalMicros() {
            return (curTime-lastTime)/1000;
        }

        public int getGrowthPerMS() {
            return (int) (((curQSize-lastQSize) * 1000) / getIntervalMicros());
        }

        @Override
        public String toString() {
            return "WorkerStats{" +
                    "lastQSize=" + lastQSize +
                    ", curQSize=" + curQSize +
                    ", nxtQSize=" + nextQSize +
                    ", growCount=" + growCount +
                    ", grow=" + getGrowthPerMS() +
                    ", iv micros=" + getIntervalMicros() +
                    '}';
        }
    }

    DefaultDispatcher workers[];
    WorkerStats wStats[];

    public DefaultScheduler(int worker) {
        workers = new DefaultDispatcher[worker];
        wStats = new WorkerStats[worker];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = (DefaultDispatcher) newDispatcher();
            wStats[i] = new WorkerStats();
            workers[i].setSystemDispatcher(true);
        }
        new Thread("Supervisor") {
            public void run() {
                while( true ) {
                    supervise();
                    try {
                        Thread.sleep(2000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        .start();
    }

    int count;
    public void supervise() {
        for (int i = 0; i < workers.length; i++) {
            DefaultDispatcher worker = workers[i];
            WorkerStats wStat = wStats[i];
            wStat.update(worker);
        }
        System.out.println("----");
        long avg = 0;
        for (int i = 0; i < workers.length; i++) {
            DefaultDispatcher worker = workers[i];
            WorkerStats wStat = wStats[i];
            avg+=wStat.curQSize;
            System.out.println(worker.getWorker().getName()+" : " + wStat);
        }
        System.out.println("avg Q =>" +avg/workers.length);
    }

    public DefaultScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public Dispatcher newDispatcher() {
        return new DefaultDispatcher();
    }

    AtomicInteger lastIndex = new AtomicInteger(-1);
    @Override
    public Dispatcher aquireDispatcher() {
        int li = lastIndex.incrementAndGet();
        if ( li > workers.length * 100 && li % workers.length == 0 )
            lastIndex.set(0);
        li %= workers.length;
        return workers[li];
    }

}

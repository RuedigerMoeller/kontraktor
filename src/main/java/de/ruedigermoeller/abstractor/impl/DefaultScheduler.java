package de.ruedigermoeller.abstractor.impl;

import de.ruedigermoeller.abstractor.ActorScheduler;
import de.ruedigermoeller.abstractor.Dispatcher;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
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

        public void update( int newQSize) {
            lastQSize = curQSize;
            curQSize = newQSize;
            lastTime = curTime;
            curTime = System.nanoTime();
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
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    int count;
    private void supervise() {
        for (int i = 0; i < workers.length; i++) {
            DefaultDispatcher worker = workers[i];
            WorkerStats wStat = wStats[i];
            wStat.update(worker.getQueueSize());
        }
        if ( count++ % 2000 == 0) {
            System.out.println("----");
            for (int i = 0; i < workers.length; i++) {
                DefaultDispatcher worker = workers[i];
                WorkerStats wStat = wStats[i];
                System.out.println(worker.getWorker().getName()+" : " + wStat);
            }
        }
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

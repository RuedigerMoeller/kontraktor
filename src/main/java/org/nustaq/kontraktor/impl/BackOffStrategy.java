package org.nustaq.kontraktor.impl;

import java.util.concurrent.locks.*;

/**
 * Created by moelrue on 06.05.2014.
 *
 * Kontraktor uses spinlocking. By adjusting the backofstrategy values one can define the tradeoff
 * regarding latency/idle CPU load.
 *
 * if a message queue is empty, first busy spin is used for N iterations, then Thread.all, then LockSupport.park, then sleep(nanosToPark)
 *
 */
public class BackOffStrategy {

    int yieldCount;
    int parkCount;
    int sleepCount;
    int nanosToPark  = 1000*300; // 1/3 milli (=latency peak on burst ..)

    public BackOffStrategy() {
        setCounters(5000,1000,10);
    }

    /**
     * @param spinUntilYield - number of busy spins until Thread.all is used
     * @param yieldUntilPark  - number of Thread.all iterations until parkNanos(1) is used
     * @param parkUntilSleep - number of parkNanos(1) is used until park(nanosToPark) is used. Default for nanosToPark is 0.5 milliseconds
     */
    public BackOffStrategy(int spinUntilYield, int yieldUntilPark, int parkUntilSleep) {
        setCounters(spinUntilYield,yieldUntilPark,parkUntilSleep);
    }

    public void setCounters( int spinUntilYield, int yieldUntilPark, int parkUntilSleep ) {
        yieldCount = spinUntilYield;
        parkCount = spinUntilYield+yieldUntilPark;
        sleepCount = spinUntilYield+yieldUntilPark+parkUntilSleep;
    }

    public int getNanosToPark() {
        return nanosToPark;
    }

    public void setNanosToPark(int nanosToPark) {
        this.nanosToPark = nanosToPark;
    }

    public void yield(int count) {
        if ( count > sleepCount || count < 0 ) {
            LockSupport.parkNanos(nanosToPark);
        } else if ( count > parkCount) {
            LockSupport.parkNanos(1);
        } else {
            if ( count > yieldCount)
                Thread.yield();
        }
    }

    public boolean isSleeping(int yieldCount) {
        return yieldCount > sleepCount;
    }
    public boolean isYielding(int count) {
        return count > yieldCount;
    }

}

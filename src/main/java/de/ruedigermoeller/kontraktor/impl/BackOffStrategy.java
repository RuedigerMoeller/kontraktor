package de.ruedigermoeller.kontraktor.impl;

import java.util.concurrent.locks.*;

/**
 * Created by moelrue on 06.05.2014.
 */
public class BackOffStrategy {

    int yieldCount;
    int parkCount;
    int sleepCount;
    int nanosToPark  = 1000*250; // half a milli

    public BackOffStrategy() {
        setCounters(100000,50000,5000);
    }

    public BackOffStrategy(int yieldCount, int parkCount, int sleepCount) {
        setCounters(yieldCount,parkCount,sleepCount);
    }

    public void setCounters( int yield, int park, int sleep ) {
        yieldCount = yield;
        parkCount = yield+park;
        sleepCount = yield+park+sleep;
    }

    public int getNanosToPark() {
        return nanosToPark;
    }

    public void setNanosToPark(int nanosToPark) {
        this.nanosToPark = nanosToPark;
    }

    public void yield(int count) {
        if ( count > sleepCount) {
            LockSupport.parkNanos(nanosToPark);
        } else if ( count > parkCount) {
            LockSupport.parkNanos(1);
        } else {
            if ( count > yieldCount)
                Thread.yield();
        }
    }


}

package de.ruedigermoeller.abstractor.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 04.01.14
 * Time: 02:54
 * To change this template use File | Settings | File Templates.
 */
public class ThreadPi {

    static double calculatePiFor(int slice, int nrOfIterations) {
        double acc = 0.0;
        for (int i = slice * nrOfIterations; i <= ((slice + 1) * nrOfIterations - 1); i++) {
            acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
        }
        return acc;
    }

    private static void piTest() throws InterruptedException {

        final int numMessages = 100000;
        final int step = 1000;

        final ExecutorService test = Executors.newFixedThreadPool(4);
        final AtomicInteger latch = new AtomicInteger(numMessages);
        final AtomicReference<Double> result = new AtomicReference<>(0.0);

        final long tim = System.currentTimeMillis();
        for ( int i= 0; i< numMessages; i++) {
            final int finalI = i;
            while ( ((ThreadPoolExecutor)test).getQueue().size() > 40000 ) {
                LockSupport.parkNanos(100);
            }
            test.execute(new Runnable() {
                public void run() {
                    double res = calculatePiFor(finalI, step);
                    Double expect;
                    boolean success;
                    do {
                        expect = result.get();
                        success = result.compareAndSet(expect,expect+res);
                        if ( ! success )
                            Thread.yield();
                    } while( !success );
                    int lc = latch.decrementAndGet();
                    if (lc == 0 ) {
                        test.shutdown();
                        System.out.println("pi: " + result.get()+" t:" + (System.currentTimeMillis() - tim)+" finI "+finalI);
                    }
                }
            });
        }
        while (latch.get() > 0 ) {
            LockSupport.parkNanos(1000*1000);
        }
    }

    public static void main( String arg[] ) throws Exception {
        for ( int i=0; i < 100; i++) {
            piTest();
        }
    }
}

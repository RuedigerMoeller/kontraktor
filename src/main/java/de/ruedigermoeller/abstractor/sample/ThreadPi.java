package de.ruedigermoeller.abstractor.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    private static long piTest(final int numThreads) throws InterruptedException {

        final int numMessages = 1000000;
        final int step = 100;

        final ExecutorService test = Executors.newFixedThreadPool(numThreads);
        final AtomicInteger latch = new AtomicInteger(numMessages);
        final AtomicReference<Double> result = new AtomicReference<>(0.0);
        final AtomicLong timSum = new AtomicLong(0);

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
                    } while( !success );
                    int lc = latch.decrementAndGet();
                    if (lc == 0 ) {
                        long l = System.currentTimeMillis() - tim;
                        timSum.set(timSum.get()+l);
                        System.out.println("pi: " + result.get() + " t:" + l + " finI " + finalI);
                        test.shutdown();
                    }
                }
            });
        }
        while (latch.get() > 0 ) {
            LockSupport.parkNanos(1000*500); // don't care as 0,5 ms are not significant per run
        }
        return timSum.get();
    }

    public static void main( String arg[] ) throws Exception {
        final int MAX_ACT = 2;
        String results[] = new String[MAX_ACT];

        for ( int numActors = 1; numActors <= MAX_ACT; numActors++ ) {
            long sum = 0;
            for ( int ii=0; ii < 30; ii++) {
                long res = piTest(numActors);
                if ( ii >= 20 ) {
                    sum+=res;
                }
            }
            results[numActors-1] = "average "+numActors+" threads : "+sum/10;
        }

        for (int i = 0; i < results.length; i++) {
            String result = results[i];
            System.out.println(result);
        }
          
    }
}

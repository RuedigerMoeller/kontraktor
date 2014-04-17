package de.ruedigermoeller.abstraktor.sample;

import java.util.concurrent.*;
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
public class ForkJoinSequentialReuse {

    static double calculatePiFor(int slice, int nrOfIterations) {
        double acc = 0.0;
        for (int i = slice * nrOfIterations; i <= ((slice + 1) * nrOfIterations - 1); i++) {
            acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
        }
        return acc;
    }

    static class MyCallable extends RecursiveTask<Double> {
        public int i;
        public int step;

        @Override
        protected Double compute() {
            double res = calculatePiFor(i, step);
            return res;
        }
    }

    private static long piTest(final int numThreads) throws InterruptedException, ExecutionException {

        final int numMessages = 1000000;
        final int step = 100;

//        final ExecutorService test = Executors.newFixedThreadPool(numThreads);
        final ForkJoinPool test = new ForkJoinPool(numThreads);
        final AtomicLong timSum = new AtomicLong(0);

        MyCallable toCall[] = new MyCallable[16000];
        for (int i = 0; i < toCall.length; i++) {
            toCall[i] = new MyCallable();
            toCall[i].step = step;
        }

        final long tim = System.currentTimeMillis();
        double res = 0;
        int count = 0;
        final int max = numMessages / toCall.length;
        for ( int i= 0; i< max; i++) {
            for ( int ii = 0; ii < toCall.length; ii++ ) {
                toCall[ii].reinitialize();
                toCall[ii].i = count++;
                test.execute(toCall[ii]);
            }
            for ( int ii= 0; ii< toCall.length; ii++) {
                res += toCall[ii].get();
            }
        }

        long l = System.currentTimeMillis() - tim;
        timSum.set(timSum.get()+l);
        System.out.println("pi: " + res + " t:" + l + " paral:"+numThreads );
        test.shutdownNow();
        return timSum.get();
    }

    public static void main( String arg[] ) throws Exception {
        final int MAX_ACT = 16;
        String results[] = new String[MAX_ACT];

        for ( int numActors = 1; numActors <= MAX_ACT; numActors++ ) {
            long sum = 0;
            for ( int ii=0; ii < 20; ii++) {
                long res = piTest(numActors);
                if ( ii >= 10 ) {
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

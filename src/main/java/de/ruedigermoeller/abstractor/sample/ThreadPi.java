package de.ruedigermoeller.abstractor.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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

    static double calculatePiFor(int start, int nrOfElements) {
        double acc = 0.0;
        for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
            acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
        }
        return acc;
    }

    private static void piTest() throws InterruptedException {

        final int numMessages = 10000000;
        final int step = 10;

        ExecutorService test = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(numMessages);
        final AtomicReference<Double> result = new AtomicReference<>(0.0);

        final long tim = System.currentTimeMillis();
        for ( int i= 0; i< numMessages; i++) {
            final int finalI = i;
            test.execute(new Runnable() {
                @Override
                public void run() {
                    double res = calculatePiFor(finalI, step);
                    Double expect = null;
                    boolean success = false;
                    do {
                        expect = result.get();
                        success = result.compareAndSet(expect,expect+res);
//                        if ( ! success )
//                            Thread.yield();
                    } while( !success );
                    latch.countDown();
                    if (latch.getCount() == 0 ) {
                        System.out.println("pi: " + result.get()+" t:" + (System.currentTimeMillis() - tim)+" finI "+finalI);
                    }
                }
            });
        }
        latch.await();
        test.shutdown();
    }

    public static void main( String arg[] ) throws Exception {
        for ( int i=0; i < 100; i++) {
            piTest();
        }
    }
}

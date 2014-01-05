package de.ruedigermoeller.fActoRy.sample;

import de.ruedigermoeller.fActoRy.Future;
import de.ruedigermoeller.fActoRy.FutureResultReceiver;
import de.ruedigermoeller.fActoRy.impl.DefaultDispatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * Date: 05.01.14
 * Time: 01:02
 * To change this template use File | Settings | File Templates.
 */

/**
 * mixing threading and actors
 */
public class MixedPi {

    public static void main( String arg[] ) throws InterruptedException {
        for (int ii = 0; ii < 100; ii++) {
            calcPi();
        }
    }

    private static void calcPi() throws InterruptedException {
        final long tim = System.currentTimeMillis();
        final int numActors = 4;
        final ExecutorService executorService = Executors.newFixedThreadPool(numActors);
        final int numMessages = 1000000;
        final int step = 100;

        final CountDownLatch finished = new CountDownLatch(1);
        final Future<Double> receiver = Future.New( numMessages, new FutureResultReceiver<Double>() {
            double pi;
            int count;
            @Override
            public void receiveDoubleResult(double result) {
                pi += result;
                count++;
                if (count == numMessages) {
                    System.out.println("pi: " + pi + " " + (System.currentTimeMillis() - tim) + " " + DefaultDispatcher.instanceCount.get());
                    finished.countDown();
                }
            }
        });
        for( int j=0; j < numMessages; j++ ) {
            final int finalI = j;
            executorService.execute(
                    new Runnable() {
                        public void run() {
                            double acc = 0.0;
                            for (int i = finalI * step; i <= ((finalI + 1) * step - 1); i++) {
                                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
                            }
                            receiver.receiveDoubleResult(acc);
                        }
                    }
            );
        }
        finished.await();
        executorService.shutdown();
    }

}

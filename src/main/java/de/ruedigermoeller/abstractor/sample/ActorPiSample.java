package de.ruedigermoeller.abstractor.sample;

import de.ruedigermoeller.abstractor.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
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
 * Time: 02:58
 * To change this template use File | Settings | File Templates.
 */
public class ActorPiSample {

    public static class PiActor extends Actor {
        public void calculatePiFor(int start, int nrOfElements, Future result ) {
            double acc = 0.0;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            result.receiveDoubleResult(acc);
        }
    }

    static long calcPi(final int numMessages, int step, int numActors) throws InterruptedException {
        final long tim = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1); // to be able to wait for finish
        final AtomicLong timSum = new AtomicLong(0);

        // setup actors, as they are not instantiated from within another actor,
        // a new dispatcher (~Thread) is created implicitely
        PiActor actors[] = new PiActor[numActors];
        for (int i = 0; i < actors.length; i++) {
            actors[i] = Actors.New(PiActor.class);
        }
        // a temporary actor to accumulate results, automatically shuts down dispatcher after numMessages
        Future resultReceiver = Future.New( numMessages,
                new FutureResultReceiver() {
                    double result;
                    int count;
                    public void receiveDoubleResult(double pi) {
                        result += pi;
                        count++;
                        if (count == numMessages) {
                            long l = System.currentTimeMillis() - tim;
                            timSum.set(l+timSum.get());
                            System.out.println("pi: " + result + " " + l);
                            latch.countDown();
                        }
                    }
                });

        for ( int i = 0; i < numMessages; i++ ) {
            actors[i%actors.length].calculatePiFor( i, step, resultReceiver);
        }
        // wait until done
        latch.await();
        // terminate/shutdown dispatchers (implicitely) created by newing actors from the non-actor world
        for (int i = 0; i < actors.length; i++) {
            actors[i].getDispatcher().shutDown();
        }
        return timSum.get();
    }

    public static void main( String arg[] ) throws InterruptedException {
        final int numMessages = 1000000;
        final int step = 100;
        final int MAX_ACT = 16;
        String results[] = new String[MAX_ACT];

        for ( int numActors = 1; numActors <= MAX_ACT; numActors++ ) {

            long sum = 0;
            for ( int ii=0; ii < 40; ii++) {
                long res = calcPi(numMessages, step, numActors);
                if ( ii >= 20 ) {
                    sum+=res;
                }
            }
            results[numActors-1] = "average "+numActors+" threads : "+sum/20;
        }

        for (int i = 0; i < results.length; i++) {
            String result = results[i];
            System.out.println(result);
        }
    }
}

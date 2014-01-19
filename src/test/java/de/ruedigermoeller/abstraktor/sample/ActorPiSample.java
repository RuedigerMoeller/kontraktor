package de.ruedigermoeller.abstraktor.sample;

import de.ruedigermoeller.abstraktor.*;
import de.ruedigermoeller.abstraktor.impl.ChannelActor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static de.ruedigermoeller.abstraktor.Actors.SpawnActor;

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
        public void calculatePiFor(int start, int nrOfElements, ChannelActor result ) {
            double acc = 0.0;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            result.receiveResult(acc);
        }
    }

    public static class PiStriper extends Actor {
        PiActor actors[];

        public void run(int numActors, int iterationSize, int numJobs, final ChannelActor resultListener ) {
            final long tim = System.currentTimeMillis();
            actors = new PiActor[numActors];
            for (int i = 0; i < actors.length; i++) {
                actors[i] = SpawnActor(PiActor.class);
            }

            final int iterPerAct = numJobs / numActors;
            final int iterSum = iterPerAct * actors.length;

            final ChannelActor endResult = Actors.QueuedChannel(new ChannelReceiver<Double>() {
                double sum = 0;
                int count = 0;

                @Override
                public void receiveResult(Double result) {
                    count++;
                    sum += result;
                    if (count == iterSum) {
                        resultListener.receiveResult(sum);
                        done();
                        PiStriper.this.getDispatcher().shutDown();
                        for (int i = 0; i < actors.length; i++) {
                            PiActor actor = actors[i];
                            actor.getDispatcher().shutDown();
                        }
                    }
                }
            });

            int iteri = 0;
            for (int i = 0; i < actors.length; i++) {
//                final ChannelActor subRes = ChannelActor.QueuedChannel(new ChannelReceiver() {
//                    double sum = 0;
//                    int count = 0;
//
//                    @Override
//                    public void receiveDoubleResult(double result) {
//                        sum += result;
//                        count++;
//                        if (count == iterPerAct) {
//                            endResult.receiveDoubleResult(sum);
//                            done();
//                        }
//                    }
//                });

                for ( int ii = 0; ii < iterPerAct; ii++ ) {
                    actors[iteri%actors.length].calculatePiFor(iteri, iterationSize, endResult /*subRes*/);
                    iteri++;
                }
            }
            System.out.println("POK iteri " + iteri);
        }

    }

    static long calcPi(final int numMessages, int step, final int numActors) throws InterruptedException {
        final long tim = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1); // to be able to wait for finish
        final AtomicLong time = new AtomicLong(0);

        ChannelActor resultReceiver = Actors.Channel(
                new ChannelReceiver<Double>() {
                    public void receiveResult(Double pi) {
                        long l = System.currentTimeMillis() - tim;
                        System.out.println("T = " + numActors + " pi: " + pi + " " + l + " disp:" + de.ruedigermoeller.abstraktor.impl.Dispatcher.instanceCount.get());
                        time.set(l);
                        done();
                        latch.countDown();
                    }
                });

        PiStriper piStriper = Actors.AsActor(PiStriper.class);
        piStriper.run(numActors,step, numMessages, resultReceiver );

        // wait until done
        latch.await();
        return time.get();
    }

    public static void main( String arg[] ) throws InterruptedException {
        final int numMessages = 100000;
        final int step = 1000;
        final int MAX_ACT = 16;
        String results[] = new String[MAX_ACT];

        for ( int numActors = 1; numActors <= MAX_ACT; numActors+=1 ) {
            long sum = 0;
            for ( int ii=0; ii < 30; ii++) {
                long res = calcPi(numMessages, step, numActors);
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

package de.ruedigermoeller.abstraktor.sample.balancing;

import de.ruedigermoeller.abstraktor.Actor;
import de.ruedigermoeller.abstraktor.Actors;
import de.ruedigermoeller.abstraktor.Future;
import de.ruedigermoeller.abstraktor.FutureResultReceiver;
import de.ruedigermoeller.abstraktor.impl.Dispatcher;

import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 1/8/14.
 */
public class WorkerActor extends Actor {

    SubActor subActors[] = new SubActor[12];
    WorkerActor self;

    public void init() {
        self = self();
        for (int i = 0; i < subActors.length; i++) {
//            subActors[i] = Actors.New( SubActor.class);
            subActors[i] = Actors.New( SubActor.class, Actors.NewDispatcher() );
        }
    }

    public void doWork(int random, final Future<String> counted) {
        final long[] count = {0};
        Future<String> fut = counted;
        for ( int i = 0; i < random; i++ ) {
            SubActor subActor = subActors[i % subActors.length];
            subActor.doAlsoWork(subActors[(i + 7) % subActors.length], fut );
        }
    }

    public void runTest(final int numMsg, final long tim, final CountDownLatch latch) {
        self.doWork(numMsg, Future.New(Actors.NewDispatcher(), true, new FutureResultReceiver<String>() {
            int count = 0;
            long res;

            @Override
            public void receiveObjectResult(String result) {
                count++;
                if (count == numMsg * 2) {
//                    System.out.println(count + " res " + res);
                    latch.countDown();
                    done();
                }
            }
        }));
    }

    public static void test() {

        int numAct = 29;
        WorkerActor act[] = new WorkerActor[numAct];
        CountDownLatch actLatch[] = new CountDownLatch[numAct];
        for (int i = 0; i < act.length; i++) {
            act[i] = Actors.New(WorkerActor.class);
            act[i].init();
            actLatch[i] = new CountDownLatch(1);
        }

        final long tim = System.nanoTime();
        final int NUMMSG = 100000;
        for (int i = 0; i < act.length; i++) {
            WorkerActor workerActor = act[i];
            workerActor.runTest(NUMMSG, tim, actLatch[i]);
        }

        try {
            for (int i = 0; i < actLatch.length; i++) {
                CountDownLatch latch = actLatch[i];
                latch.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("tim " + (System.nanoTime() - tim) / 1000 / 1000 );
    }

    public static void main( String arg[] ) throws InterruptedException {
        for ( int i = 0; i < 50; i++) {
            test();
            System.out.println( "dispatcher: "+ Dispatcher.instanceCount.get() );
        }
        System.exit(0);
    }

}

package kontraktor.scheduling.exectest;

import java.util.concurrent.*;
import static kontraktor.scheduling.exectest.Runner2.Mode.Dedicated;
import static kontraktor.scheduling.exectest.Runner2.Mode.FixedThread;
import static kontraktor.scheduling.exectest.Runner2.Mode.WorkStealing;

/**
 * Created by ruedi on 10/13/14.
 *
 * Note this test does not make sense for machines with less then 8 cores (L1 cache misses will not be significant)
 * However with MAX_WORKER = 4, cache effects still show up, but to a lesser extent
 */
public class Runner2 {


    enum Mode {
        WorkStealing,
        FixedThread,
        Dedicated
    }

    final static int MAX_WORKER = 8;
    static final int NUM_ACTORS_PER_WORKER = 10;
    static int memAcc = 100;

    Work2 workers[] = new Work2[MAX_WORKER*NUM_ACTORS_PER_WORKER]; // 10 actors per thread

    ExecutorService ex;

    ExecutorService threads[] = new ExecutorService[MAX_WORKER];
    Mode mode = Mode.Dedicated;

    public Runner2(Mode mode) {
        this.mode = mode;
    }

    public Runner2 init(int localSize) {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Work2(localSize);
            if ( mode == Mode.Dedicated && i < MAX_WORKER ) {
                threads[i] = Executors.newSingleThreadScheduledExecutor();
            }
        }
        switch (mode) {
            case WorkStealing:
                ex = Executors.newWorkStealingPool(MAX_WORKER); break;
            case FixedThread:
                ex  = Executors.newFixedThreadPool(MAX_WORKER); break;
        }
        return this;
    }

    public long run(int iter) throws InterruptedException {
        long tim = System.currentTimeMillis();
        CountDownLatch finSignal = new CountDownLatch(workers.length);
        for ( int i = 0; i < workers.length; i++ ) {
            final int finalI = i;
            if ( mode == Mode.Dedicated ) {
                ExecutorService dedicatedThread = threads[finalI % MAX_WORKER];
                dedicatedThread.execute(() -> workers[finalI % workers.length].doWork(memAcc, dedicatedThread, iter/workers.length, finSignal ));
            } else {
                ex.execute( () -> workers[finalI % workers.length].doWork(memAcc,ex, iter/workers.length, finSignal) );
            }
        }
        finSignal.await();
        long dur = System.currentTimeMillis()-tim;
//        System.out.println(mode+" Time "+dur);
        return dur;
    }

    void shutdown() throws InterruptedException {
        if ( mode == Mode.Dedicated ) {
            for (int i = 0; i < threads.length; i++) {
                ExecutorService thread = threads[i];
                thread.shutdown();
                thread.awaitTermination(10L, TimeUnit.SECONDS);
            }
        } else {
            ex.shutdown();
            ex.awaitTermination(10L, TimeUnit.SECONDS);
        }
    }

    private static long avgTest(Mode mode, int localSize ) throws InterruptedException {
        long sum = 0;
        Runner2 runner = new Runner2(mode).init(localSize);
        int iters = 1;
        for (int i = 0; i < iters; i++) {
            sum += runner.run(1000 * 1000 * 5);
//            Thread.sleep(1000);
        }
//        System.out.println();
        System.out.println("*** "+mode + " average "+sum/iters+"  localSize "+localSize*4);
//        System.out.println();
        runner.shutdown();
        return sum/iters;
    }

    public static void main(String arg[]) throws InterruptedException {
        int sizes[] = { 16, 64, 500, 1000, 8000, 80000 };
        long durations[][] = new long[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            for ( int ii = 0; ii < 2; ii++ ) {
                System.out.println("warmup =>");
                avgTest(Dedicated, size);
                avgTest(FixedThread, size);
                avgTest(WorkStealing, size);
            }
            durations[i] =  new long[3];
            int numRuns = 3;
            for ( int ii = 0; ii < numRuns; ii++ ) {
                System.out.println("run => "+ii);
                durations[i][Dedicated.ordinal()] += avgTest(Dedicated, size);
                durations[i][FixedThread.ordinal()] += avgTest(FixedThread, size);
                durations[i][WorkStealing.ordinal()] += avgTest(WorkStealing, size);
            }
            for (int j = 0; j < durations[i].length; j++) {
                durations[i][j] /= numRuns;
            }
        }
        System.out.println("Final results ************** Worker Threads:"+MAX_WORKER+" actors:"+(MAX_WORKER*NUM_ACTORS_PER_WORKER)+" #mem accesses: "+memAcc );
        for (int i = 0; i < durations.length; i++) {
            long[] duration = durations[i];
            for (int j = 0; j < 3; j++) {
                System.out.println("local state bytes: "+sizes[i]*4+" "+Mode.values()[j]+" avg:"+duration[j]);

            }
        }
    }

}

package kontraktor.scheduling.exectest;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.util.Hoarde;
import org.nustaq.kontraktor.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static kontraktor.scheduling.exectest.Runner2.Mode.Dedicated;
import static kontraktor.scheduling.exectest.Runner2.Mode.FixedThread;
import static kontraktor.scheduling.exectest.Runner2.Mode.WorkStealing;

/**
 * Created by ruedi on 16.10.2014.
 */
public class ActorRunner {

    final static int MAX_WORKER = 8;
    static final int NUM_ACTORS_PER_WORKER = 10;
    static int memAcc = 100;

    public long run(int act, int th, int siz, int iter) throws InterruptedException {
        ElasticScheduler sched = new ElasticScheduler(th,5000);
        Hoarde<WorkActor> h = new Hoarde<>(act, WorkActor.class, sched);
        long tim = System.currentTimeMillis();
        CountDownLatch finSignal = new CountDownLatch(1);
        h.each( (worker) -> worker.$init(siz) );
        for ( int n=0; n < iter; n++ )
            h.each( (worker) -> worker.$doWork(memAcc) );

        Actors.yield( h.map( (wk,i) -> wk.$sync() ) ).then( (r,e) -> finSignal.countDown() );
        finSignal.await();
        long dur = System.currentTimeMillis()-tim;
        h.each( (worker) -> worker.$stop() );
        System.out.println(" Time "+dur);
        return dur;
    }

    private static long avgTest(int localSize ) throws InterruptedException {
        long sum = 0;
        ActorRunner runner = new ActorRunner();
        int iters = 1;
        for (int i = 0; i < iters; i++) {
            sum += runner.run(MAX_WORKER*NUM_ACTORS_PER_WORKER, MAX_WORKER, localSize, (1000 * 1000 * 5) / MAX_WORKER / NUM_ACTORS_PER_WORKER);
        }
        System.out.println("*** Actor average " + sum / iters + "  localSize " + localSize * 4);
        return sum/iters;
    }

    public static void main(String arg[]) throws InterruptedException {
        Log.Lg.$setSeverity(Log.ERROR);
        int sizes[] = { 16, 64, 500, 1000, 8000, 80000 };
        long durations[][] = new long[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            for ( int ii = 0; ii < 2; ii++ ) {
                System.out.println("warmup =>");
                avgTest(size);
            }
            durations[i] =  new long[1];
            int numRuns = 3;
            for ( int ii = 0; ii < numRuns; ii++ ) {
                System.out.println("run => "+ii);
                durations[i][0] += avgTest(size);
            }
            for (int j = 0; j < durations[i].length; j++) {
                durations[i][j] /= numRuns;
            }
        }
        System.out.println("Final results ************** Worker Threads:"+MAX_WORKER+" actors:"+(MAX_WORKER*NUM_ACTORS_PER_WORKER)+" #mem accesses: "+memAcc );
        for (int i = 0; i < durations.length; i++) {
            long[] duration = durations[i];
            for (int j = 0; j < 1; j++) {
                System.out.println("local state bytes: "+sizes[i]*4+" avg:"+duration[j]);

            }
        }
    }


}

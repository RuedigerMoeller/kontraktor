package de.ruedigermoeller.abstraktor.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * my shot on sequential FJ jobs, does not scale, but better single thread performance
 */
public class ForkJoinReuse {

    static class PiForkJoinTask extends RecursiveTask<Double> {
        private int slice;

        @Override
        protected Double compute() {
            return ForkJoinRecursiveDeep.calculatePi(slice);
        }
    }

    public double run() throws InterruptedException {
        List<PiForkJoinTask> tasks = new ArrayList<>();

        int stride = CurCores * 100;
        for (int i = 0; i < stride; i++) {
            PiForkJoinTask task = new PiForkJoinTask();
            task.slice = i;
            task.fork();
            tasks.add(task);
        }

        double acc = 0D;
        int s = stride;
        while (s < ForkJoinRecursiveDeep.SLICES) {
            for (PiForkJoinTask task : tasks) {
                acc += task.join();
                task.reinitialize();
                task.slice = s;
                task.fork();
            }
            s += stride;
        }

        for (PiForkJoinTask task : tasks) {
            acc += task.join();
        }

        return acc;
    }

    static ForkJoinPool pool;
    static int CurCores;
    public static void main(String arg[] ) throws InterruptedException, ExecutionException {
        int NUM_CORE = 16;
        String res[] = new String[NUM_CORE];
        for ( int i = 1; i <= NUM_CORE; i++ ) {
            long sum = 0;
            CurCores = i;
            System.out.println("--------------------------");
            pool = new ForkJoinPool(i);
            for ( int ii = 0; ii < 20; ii++ ) {
                long tim = System.currentTimeMillis();
                final ForkJoinReuse dis = new ForkJoinReuse();
                ForkJoinTask<Double> submit = pool.submit(
                        new Callable<Double>() {
                            @Override
                            public Double call() {
                                try {
                                    return dis.run();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }
                );
                Double aDouble = submit.get();
                long t = System.currentTimeMillis()-tim;
                System.out.println("pi "+ aDouble +" t "+t+" threads:"+i);
                if ( ii >= 10 )
                    sum += t;
            }
            res[i-1] = i+": "+(sum/10);
            pool.shutdown();
        }
        for (int i = 0; i < res.length; i++) {
            String re = res[i];
            System.out.println(re);
        }
    }

}


package de.ruedigermoeller.abstraktor.sample;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * originally created by Aleksey Shipilev
 * 
 * added some minor correction to actually compute Pi, adapted to blog benchmark mainloop 'style'
 * 
 */
public class ForkJoinRecursiveDeep {

    public static double calculatePi(int sliceNr) {
        double acc = 0.0;
        for (int i = sliceNr * ITERS; i <= ((sliceNr + 1) * ITERS - 1); i++) {
            acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
        }
        return acc;
    }
    
    /*
      The fork-join task below deeply recurses, up until the leaf
      contains a single slice.
     */

    static class PiForkJoinTask extends RecursiveTask<Double> {
        private final int slices;
        private final int slicesOffset;

        public PiForkJoinTask(int slices, int offset) {
            this.slices = slices;
            this.slicesOffset = offset;
        }

        @Override
        protected Double compute() {
            if ( slices == 0 )
                return 0.0;
            if (slices == 1) {
                return calculatePi(slicesOffset);
            }

            int lslices = slices / 2;
            int rslices = slices - lslices;
            PiForkJoinTask t1 = new PiForkJoinTask(lslices,slicesOffset);
            PiForkJoinTask t2 = new PiForkJoinTask(rslices,lslices+slicesOffset);

            ForkJoinTask.invokeAll(t1, t2);

            return t1.join() + t2.join();
        }
    }

    public double run() throws InterruptedException {
        return pool.invoke(new PiForkJoinTask(SLICES,0));
    }

    private static final int ITERS = 100;
    private static final int SLICES = 1000*1000;

    static ForkJoinPool pool;
    public static void main(String arg[] ) throws InterruptedException {

        int NUM_CORE = 16;
        String res[] = new String[NUM_CORE];
        for ( int i = 1; i <= NUM_CORE; i++ ) {
            long sum = 0;
            System.out.println("--------------------------");
            pool = new ForkJoinPool(i);
            for ( int ii = 0; ii < 20; ii++ ) {
                long tim = System.currentTimeMillis();
                final ForkJoinRecursiveDeep dis = new ForkJoinRecursiveDeep();
                System.out.println(dis.run());
                long t = System.currentTimeMillis()-tim;
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

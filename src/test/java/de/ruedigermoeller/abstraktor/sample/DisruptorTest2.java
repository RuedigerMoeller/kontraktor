package de.ruedigermoeller.abstraktor.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Created by ruedi on 24.01.14.
 */
public class DisruptorTest2 {

    public static class PiJob {
        public double result;
        public int sliceNr;
        public int numIter;
        public int partionId;

        public void calculatePi() {
            double acc = 0.0;
            for (int i = sliceNr * numIter; i <= ((sliceNr + 1) * numIter - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            result = acc;
        }

    }

    public static class PiEventFac implements EventFactory<PiJob> {

        @Override
        public PiJob newInstance() {
            return new PiJob();
        }
    }

    public static class PiEventProcessor implements EventHandler<PiJob> {

        private int partionId;

        public PiEventProcessor(int partionId) {
            this.partionId = partionId;
        }

        @Override
        public void onEvent(PiJob event, long sequence, boolean isEndOfBatch) throws Exception {
            if (partionId == event.partionId) {
                event.calculatePi();
            }
        }
    }

    public static class PiResultReclaimer implements EventHandler<PiJob> {
        double result;
        public long seq = 0;
        private final int numSlice;
        final CountDownLatch latch;

        public PiResultReclaimer(int numSlice)
        {
            this.numSlice = numSlice;
            latch = new CountDownLatch(1);
        }

        @Override

        public void onEvent(PiJob event, long sequence, boolean isEndOfBatch) throws Exception {
            result += event.result;
            ++seq;

            if (seq >= numSlice) {
                latch.countDown();
            }
        }
    }

    public long run(int numTH, int numSlice, int numIter) throws InterruptedException {
        PiEventFac fac = new PiEventFac();
        ExecutorService executor = Executors.newCachedThreadPool();
        Disruptor<PiJob> disruptor = new Disruptor<>(fac,16384, executor, ProducerType.SINGLE, new SleepingWaitStrategy());
        PiEventProcessor procs[] = new PiEventProcessor[numTH];
        PiResultReclaimer res = new PiResultReclaimer(numSlice);

        for (int i = 0; i < procs.length; i++) {
            procs[i] = new PiEventProcessor(i);
        }

        disruptor.handleEventsWith(procs).then(res);

        disruptor.start();

        final RingBuffer<PiJob> ringBuffer = disruptor.getRingBuffer();
        long tim = System.currentTimeMillis();
        int partionId = 0;
        for (int i= 0; i < numSlice; i++ ) {
            final long seq = ringBuffer.next();
            final PiJob piJob = ringBuffer.get(seq);
            piJob.numIter = numIter;
            piJob.sliceNr = i;
            piJob.result = 0;
            piJob.partionId = partionId;
            ringBuffer.publish(seq);

            partionId = (partionId == (numTH - 1)) ? 0 : partionId + 1;
        }

        res.latch.await();

        long timTest = System.currentTimeMillis() - tim;
        System.out.println(numTH+": tim: "+ timTest +" Pi: "+res.result);

        disruptor.shutdown();
        executor.shutdownNow();
        return timTest;
    }

    public static void main(String arg[] ) throws InterruptedException {
        final DisruptorTest2 disruptorTest = new DisruptorTest2();
        int numSlice = 1000000;
        int numIter = 100;

        int NUM_CORE = 16;
        String res[] = new String[NUM_CORE];
        for ( int i = 1; i <= NUM_CORE; i++ ) {
            long sum = 0;
            System.out.println("--------------------------");
            for ( int ii = 0; ii < 20; ii++ ) {
                long t = disruptorTest.run(i, numSlice, numIter);
                if ( ii >= 10 )
                    sum += t;
            }
            res[i-1] = i+": "+(sum/10);
        }
        for (int i = 0; i < res.length; i++) {
            String re = res[i];
            System.out.println(re);
        }
    }

}

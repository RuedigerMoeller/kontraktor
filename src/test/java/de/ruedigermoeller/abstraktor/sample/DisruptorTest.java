package de.ruedigermoeller.abstraktor.sample;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 24.01.14.
 */
public class DisruptorTest {

    public static class PiJob {
        public double result;
        public int sliceNr;
        public int numIter;

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

    public static class PiEventProcessor implements WorkHandler<PiJob> {
        @Override
        public void onEvent(PiJob event) throws Exception {
            event.calculatePi();
        }
    }

    public static class PiResultReclaimer implements WorkHandler<PiJob> {
        double result;
        public AtomicLong seq = new AtomicLong(0);

        @Override
        public void onEvent(PiJob event) throws Exception {
            result += event.result;
            seq.incrementAndGet();
        }
    }

    public long run(int numTH, int numSlice, int numIter) throws InterruptedException {
        PiEventFac fac = new PiEventFac();
        ExecutorService executor = Executors.newCachedThreadPool();
        Disruptor<PiJob> disruptor = new Disruptor<>(fac,16384, executor, ProducerType.SINGLE, new BlockingWaitStrategy());
        PiEventProcessor procs[] = new PiEventProcessor[numTH];
        PiResultReclaimer res = new PiResultReclaimer();

        for (int i = 0; i < procs.length; i++) {
            procs[i] = new PiEventProcessor();
        }

        disruptor.handleEventsWithWorkerPool(procs).thenHandleEventsWithWorkerPool(res);

        disruptor.start();

        final RingBuffer<PiJob> ringBuffer = disruptor.getRingBuffer();
        long tim = System.currentTimeMillis();
        for (int i= 0; i < numSlice; i++ ) {
            final long seq = ringBuffer.next();
            final PiJob piJob = ringBuffer.get(seq);
            piJob.numIter = numIter;
            piJob.sliceNr = i;
            piJob.result = 0;
            ringBuffer.publish(seq);
        }
        while (res.seq.get() < numSlice)
        {
            Thread.sleep(1);
            // busy spin
        }
        long timTest = System.currentTimeMillis() - tim;
        System.out.println(numTH+": tim: "+ timTest +" Pi: "+res.result);

        disruptor.shutdown();
        executor.shutdownNow();
        return timTest;
    }

    public static void main(String arg[] ) throws InterruptedException {
        final DisruptorTest disruptorTest = new DisruptorTest();
        int numSlice = 100000;
        int numIter = 1000;

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

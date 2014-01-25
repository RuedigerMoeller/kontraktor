package de.ruedigermoeller.abstraktor.sample;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

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

    public void run(int numTH, int numSlice, int numIter) throws InterruptedException {
        PiEventFac fac = new PiEventFac();
        Disruptor<PiJob> disruptor = new Disruptor<>(fac,16384, Executors.newCachedThreadPool());
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
        System.out.println(numTH+": tim: "+(System.currentTimeMillis()-tim)+" Pi: "+res.result);

        disruptor.shutdown();

    }

    public static void main(String arg[] ) throws InterruptedException {
        final DisruptorTest disruptorTest = new DisruptorTest();

        for ( int i = 1; i <= 4; i++ ) {
            System.out.println("--------------------------");
            for ( int ii = 0; ii < 30; ii++ ) {
                disruptorTest.run(i,1000000,100);
            }
        }
    }

}

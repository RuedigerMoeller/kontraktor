package de.ruedigermoeller.disruptorbench;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 20.04.14.
 */
public class DisruptorService implements LoadFeeder.Service {

    static AtomicInteger decount = new AtomicInteger(0); // debug

    public static class DSEventFac implements EventFactory<TestRequest> {
        @Override
        public TestRequest newInstance() {
            return new TestRequest();
        }
    }

    public static class DSPartitionedProcessor implements EventHandler<TestRequest> {
        int part;

        public DSPartitionedProcessor(int part) {
            this.part = part;
        }

        @Override
        public void onEvent(TestRequest event, long sequence, boolean isEndOfBatch) throws Exception {
            if ( event.decPartition == part )
                event.decode();
        }
    }

    Disruptor<TestRequest> disruptor;
    ExecutorService executor;
    SharedData sharedData;

    int decPartCount = 0;
    int encPartCount = 0;
    int numDecodingThreads;
    int numEncodingThreads;

    public DisruptorService(LoadFeeder feeder, int numDecodingThreads, int numEncodingThreads, SharedData sharedData) {
        this.numDecodingThreads = numDecodingThreads;
        this.numEncodingThreads = numEncodingThreads;
        this.sharedData = sharedData;
        initDisruptor(feeder);
    }

    void initDisruptor(final LoadFeeder feeder) {
        executor = Executors.newCachedThreadPool();
        disruptor = new Disruptor<>(new DSEventFac(), 65536, executor, ProducerType.SINGLE, new SleepingWaitStrategy());
//        DSPartitionedProcessor decoders[] = new DSPartitionedProcessor[codingThreads];
        DSPartitionedProcessor decoders[] = new DSPartitionedProcessor[numDecodingThreads];
        for (int i = 0; i < decoders.length; i++) {
            decoders[i] = new DSPartitionedProcessor(i);
        }
        DSPartitionedProcessor encoders[] = new DSPartitionedProcessor[numEncodingThreads];
        for (int i = 0; i < encoders.length; i++) {
            encoders[i] = new DSPartitionedProcessor(i) {
                @Override
                public void onEvent(TestRequest event, long sequence, boolean isEndOfBatch) throws Exception {
                    if ( event.encPartition == part )
                        event.encode(feeder);
                }
            };
        }
        disruptor
                .handleEventsWith(decoders)
                .then(new EventHandler<TestRequest>() {
                    @Override
                    public void onEvent(TestRequest event, long sequence, boolean endOfBatch) throws Exception {
                        event.process(sharedData);
                    }
                })
                .handleEventsWith(encoders)
        ;
        disruptor.start();
    }

    @Override
    public void processRequest(byte[] b) {
        final RingBuffer<TestRequest> ringBuffer = disruptor.getRingBuffer();
        final long seq = ringBuffer.next();
        final TestRequest requestEntry = ringBuffer.get(seq);
        requestEntry.rawRequest = b;
        requestEntry.decPartition = decPartCount++;
        requestEntry.encPartition = encPartCount++;
        if ( decPartCount == numDecodingThreads )
            decPartCount = 0;
        if ( encPartCount == numEncodingThreads )
            encPartCount = 0;
        ringBuffer.publish(seq);
    }

    @Override
    public void shutdown() {
        disruptor.shutdown();
        executor.shutdownNow();
    }

    public static void main(String a[]) throws IOException, ClassNotFoundException {
        for ( int i = 0; i < 50; i++ ) {
            LoadFeeder feeder = new LoadFeeder(10000);
            DisruptorService service = new DisruptorService(feeder, 3, 3, new SingleThreadedSharedData());
            feeder.run(service, 2 * 1000 * 1000);
        }
    }


}

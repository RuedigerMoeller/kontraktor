package de.ruedigermoeller.disruptorbench;

import de.ruedigermoeller.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 4/21/14.
 */
public class QueuedThreadPoolExecutor implements LoadFeeder.Service{

    static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration();
    private LoadFeeder serv;
    private ExecutorService decodeExecutor;
    private ExecutorService encodeExecutor;
    private ExecutorService logicExecutor;

    private static class TestThread extends Thread {
        public TestRequest req = new TestRequest();
        public TestThread(Runnable r, String name) {
            super(r,name);
        }
    }

    ThreadLocal<LoadFeeder.Response> respTLocal = new ThreadLocal() {
        @Override
        protected LoadFeeder.Response initialValue() {
            return new LoadFeeder.Response(null,0);
        }
    };

    SharedData sharedData;

    public QueuedThreadPoolExecutor(LoadFeeder serv, SharedData data, int numEnc, int numDec) {
        this.serv = serv;
        init(numEnc,numDec);
        sharedData = data;
    }

    public void init(int decoders, int encoders) {
        decodeExecutor = (ExecutorService) createBoundedThreadExecutor(decoders, "pooldec", 40000);
        encodeExecutor = (ExecutorService) createBoundedThreadExecutor(encoders, "poolenc", 40000);
        logicExecutor = (ExecutorService) createBoundedThreadExecutor(1, "pool-logic", 40000);
//        decodeExecutor = Executors.newFixedThreadPool(workers,new ThreadFactory() {
//            @Override
//            public Thread newThread(Runnable r) {
//                return new TestThread( r, "-" );
//            }
//        });
    }

    @Override
    public void processRequest(final byte[] rawRequest) {
        decodeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final TestRequest testRequest = ((TestThread) Thread.currentThread()).req;
                    testRequest.rawRequest = rawRequest;
                    testRequest.decode();
                    encodeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                testRequest.process(sharedData);
                                testRequest.encode(serv);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        decodeExecutor.shutdownNow();
        try {
            decodeExecutor.awaitTermination(10000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Executor createBoundedThreadExecutor(int workers, final String name, int qsize) {
        ThreadPoolExecutor res = new ThreadPoolExecutor(workers,workers,1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(qsize));
//        ThreadPoolExecutor res = new ThreadPoolExecutor(1,1,1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(qsize));
        res.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new TestThread( r, name );
            }
        });
        res.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                while ( !executor.isShutdown() && !executor.getQueue().offer(r)) {
                    LockSupport.parkNanos(1000);
                }
            }
        } );
        return res;
    }

    public static void main(String a[]) throws IOException, ClassNotFoundException {
        for ( int i = 0; i < 50; i++ ) {
            LoadFeeder feeder = new LoadFeeder(10000);
            QueuedThreadPoolExecutor service = new QueuedThreadPoolExecutor(feeder,new LockFreeSharedData(),3,3);
//            UnorderedThreadPoolService service = new UnorderedThreadPoolService(feeder,new SynchronizedSharedData(),2);
            feeder.run(service, 1000 * 1000);
        }
    }

}

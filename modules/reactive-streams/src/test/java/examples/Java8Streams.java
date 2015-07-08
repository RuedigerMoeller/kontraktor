package examples;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.reactivestreams.CancelException;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 08/07/15.
 */
public class Java8Streams {


    /**
     * stream a java 8 stream via network
     *
     * @throws InterruptedException
     */
    public <T> int remoteJava8Streams( Function<MyEvent,T> doStuff, int port ) throws InterruptedException {

        AtomicInteger validCount = new AtomicInteger(-1);

        KxReactiveStreams.get()
            .produce(IntStream.range(0, 5_000_000).mapToObj(i -> new MyEvent(i,Math.random(),"Hello"+i)))
            .serve( new TCPPublisher().port(port) );

        CountDownLatch latch = new CountDownLatch(1);

        RateMeasure measure = new RateMeasure("rate");
        KxReactiveStreams.get()
            .connect( MyEvent.class, new TCPConnectable().host("localhost").port(port) )
            .stream(stream -> {
                long count = 0;
                try {
                    count = stream
                        .map(event -> {
                            measure.count();
                            return doStuff.apply(event);
                        })
                        .count();
                } finally {
                    System.out.println("Count:" + count);
                    validCount.set((int) count);
                    latch.countDown();
                }
            });

        latch.await();
        return validCount.get();
    }

    @Test
    public void java8streamsRemote() throws InterruptedException {
        int res = remoteJava8Streams(x -> x, 8123 );
        System.out.println("res:"+res);
        Assert.assertTrue( res == 5_000_000 );
    }

    @Test
    public void java8streamsRemoteAndCancel() throws InterruptedException {
        int res =
            remoteJava8Streams( x -> {
                if ( x.num == 2_000_000 )
                    throw CancelException.Instance;
                return x;
            }, 8124 );
        Assert.assertTrue( res == 0 );
    }

    @Test @org.junit.Ignore // just manual
    public <T> void remoteJava8EndlessStreams() throws InterruptedException {
        EventSink sink = new EventSink();
        sink.serve(new TCPPublisher().port(8125));

        runTimeClient("c1");
        runTimeClient("c2");
        runTimeClient("c3");

        for ( int i = 0; i < 1000; i++ ) {
            while( ! sink.offer(new Date()) ) {
                Thread.sleep(1);
            }
            Thread.sleep(1000);
        }
    }

    public void runTimeClient(String tag) {
        KxReactiveStreams.get()
            .connect( Date.class, new TCPConnectable().host("localhost").port(8125) )
            .stream( stream -> {
                stream.forEach(date -> {
                    System.out.println(tag+" "+date);
                    if (Math.random() < 0.1) {
                        throw CancelException.Instance;
                    }
                });
            });
    }

}

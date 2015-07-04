package misc;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.reactivestreams.EventGenerator;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.util.RateMeasure;
import org.xnio.streams.Streams;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by ruedi on 04/07/15.
 */
public class Basics {

    @Test
    public void testStopThreads() throws InterruptedException {

        EventSink<Integer> eventSink = new EventSink<Integer>();

        AtomicInteger sum = new AtomicInteger(0);

        RateMeasure ms = new RateMeasure("rate");
        eventSink
            .map(i -> i * i)
            .map(i -> "" + i)
            .map( s -> s.substring(0, s.length() / 2))
            .map(s -> s.length())
            .subscribe((len, err) -> {
                ms.count();
                if (len != null) // complete signal
                    sum.addAndGet(len);
            });

        for ( int i = 0; i < 20_000_000; i++ ) {
            while( ! eventSink.offer(i) ) {
                Thread.yield();
            }
        }
        eventSink.complete();
        System.out.println("count:" + DispatcherThread.activeDispatchers.get());

        Thread.sleep(1000);
        System.out.println("sum:" + sum.get());

        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == 1);
    }

    @Test
    public void testStopThreadsWithIter() throws InterruptedException {


        AtomicInteger sum = new AtomicInteger(0);

        RateMeasure ms = new RateMeasure("rate");
        EventGenerator.of(IntStream.range(0,20_000_000).mapToObj(i -> i))
            .map(i -> i * i)
            .map(i -> "" + i)
            .map( s -> s.substring(0, s.length() / 2))
            .map(s -> s.length())
            .subscribe((len, err) -> {
                ms.count();
                if (len != null) // complete signal
                    sum.addAndGet(len);
            });

        System.out.println("count:" + DispatcherThread.activeDispatchers.get());

        Thread.sleep(1000);
        while( true ) {
            Thread.sleep(1000);
            System.out.println("sum:" + sum.get());
        }

//        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == 1);
    }

}

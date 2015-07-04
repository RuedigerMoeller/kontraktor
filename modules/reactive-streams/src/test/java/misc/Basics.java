package misc;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 04/07/15.
 */
public class Basics {

    public static final int MASMSG_NUM = 2_000_000;
    public static final int NETWORK_MSG = 2_000_000;

    @Test
    public void testStopThreads() throws InterruptedException {

        resetThreadCount();

        EventSink<Integer> eventSink = new EventSink<Integer>();

        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger cnt = new AtomicInteger(0);

        RateMeasure ms = new RateMeasure("rate");
        eventSink
            .map(i -> i * i)
            .asyncMap(i -> "" + i)
            .map( s -> s.substring(0, s.length() / 2))
            .asyncMap(s -> s.length())
            .subscribe((len, err) -> {
                ms.count();
                if (len != null) { // complete signal
                    cnt.incrementAndGet();
                    sum.addAndGet(len);
                }
            });

        for ( int i = 0; i < MASMSG_NUM; i++ ) {
            while( ! eventSink.offer(i) ) {
                Thread.yield();
            }
        }
        eventSink.complete();

        int count = 0;
        while( cnt.get() != MASMSG_NUM && count < 10) {
            Thread.sleep(1000);
            count++;
        }

        Thread.sleep(5000);

        System.out.println("threads:" + DispatcherThread.activeDispatchers.get());
        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == 0);
    }

    public void resetThreadCount() throws InterruptedException {
        Log.Lg.info(this, "resetting");
        Thread.sleep(5000);
        Log.Lg.info(null, "initial count:" + DispatcherThread.activeDispatchers.get());
        DispatcherThread.activeDispatchers.set(0);
    }

    @Test
    public void testStopThreadsError() throws InterruptedException {

        resetThreadCount();

        EventSink<Integer> eventSink = new EventSink<Integer>();

        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger elems = new AtomicInteger(0);

        RateMeasure ms = new RateMeasure("rate");
        eventSink
            .map(i -> i * i)
            .asyncMap(i -> "" + i)
            .map( s -> s.substring(0, s.length() / 2))
            .asyncMap(s -> s.length())
            .subscribe((len, err) -> {
                ms.count();
                if (len != null) { // complete signal
                    elems.incrementAndGet();
                    sum.addAndGet(len);
                }
            });

        for ( int i = 0; i < MASMSG_NUM; i++ ) {
            while( ! eventSink.offer(i) ) {
                Thread.yield();
            }
        }
        eventSink.error(new RuntimeException("error"));
        System.out.println("count:" + DispatcherThread.activeDispatchers.get());

        int cnt = 0;
        while( sum.get() != 94803201 && cnt < 10) {
            Thread.sleep(1000);
            cnt++;
        }

        System.out.println("count:" + DispatcherThread.activeDispatchers.get());
        Thread.sleep(1000);

        System.out.println("elems:" + elems.get());
        Thread.sleep(5000);

        Assert.assertTrue(elems.get() == MASMSG_NUM);
        System.out.println("disp:" + DispatcherThread.activeDispatchers.get());
        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == 0);
    }

    @Test
    public void testStopThreadsWithIter() throws InterruptedException {
        resetThreadCount();

        AtomicInteger sum = new AtomicInteger(0);
        AtomicInteger elems = new AtomicInteger(0);

        RateMeasure ms = new RateMeasure("rate");
        ReaktiveStreams.get().produce(IntStream.range(0, MASMSG_NUM))
            .map(i -> i * i)
            .asyncMap(i -> "" + i)
            .map( s -> s.substring(0, s.length() / 2))
            .asyncMap(s -> s.length())
            .subscribe((len, err) -> {
                ms.count();
                if (len != null) { // complete signal
                    sum.addAndGet(len);
                    elems.incrementAndGet();
                }
            });

        System.out.println("count:" + DispatcherThread.activeDispatchers.get());

        int cnt = 0;
        while( sum.get() != 94803201 && cnt < 10) {
            Thread.sleep(1000);
            cnt++;
        }

        System.out.println("count:" + DispatcherThread.activeDispatchers.get());
        Thread.sleep(5000);
        Assert.assertTrue(elems.get() == MASMSG_NUM);
        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == 0);
    }

    @Test
    public void testConnectionCloseOnCompleteTCP() throws InterruptedException {

        TCPPublisher publisher = new TCPPublisher().port(7855);
        TCPConnectable connectable = new TCPConnectable().host("localhost").port(7855);

        concloseTest(publisher, connectable);
    }

    @Test
    public void testConnectionCloseOnCompleteWS() throws InterruptedException {

        WebSocketPublisher publisher = new WebSocketPublisher().hostName("localhost").urlPath("/ws").port(8082);
        ConnectableActor connectable = new WebSocketConnectable().url("ws://localhost:8082/ws");

        concloseTest(publisher, connectable);
    }

    @Test
    public void testConnectionCloseOnCompleteNIO() throws InterruptedException {

        TCPNIOPublisher publisher = new TCPNIOPublisher().port(7854);
        TCPConnectable connectable = new TCPConnectable().host("localhost").port(7854);

        concloseTest(publisher, connectable);
    }

    public void concloseTest(ActorPublisher publisher, ConnectableActor connectable) throws InterruptedException {
        ReaktiveStreams.get()
            .produce(IntStream.range(0, NETWORK_MSG))
            .serve(publisher);

        AtomicInteger cnt = new AtomicInteger(0);
        ReaktiveStreams.get()
            .connect(Integer.class, connectable)
            .subscribe((r, e) -> {
                if (Actors.isResult(e)) {
                    cnt.incrementAndGet();
                } else {
                    System.out.println("not result " + r + " " + e);
                }
            });

        System.out.println("thread:" + DispatcherThread.activeDispatchers.get());

        int count = 0;
        while( count < 10 && cnt.get() != NETWORK_MSG ) {
            Thread.sleep(1000);
            System.out.println("received msg:" + cnt.get());
            count++;
        }

        Assert.assertTrue(cnt.get() == NETWORK_MSG);

        System.out.println("thread:" + DispatcherThread.activeDispatchers.get());
    }

    @Test
    public void testConnectionCloseOnCompleteWithSink() throws InterruptedException {

        EventSink<Integer> sink = new EventSink<>();
        sink
            .serve(new TCPPublisher().port(7850));

        AtomicInteger cnt = new AtomicInteger(0);
        ReaktiveStreams.get()
            .connect(Integer.class, new TCPConnectable().host("localhost").port(7850))
            .subscribe((r, e) -> {
                if (Actors.isResult(e)) {
                    cnt.incrementAndGet();
                } else {
                    System.out.println("not result " + r + " " + e);
                }
            });

        for ( int i = 0; i < 100_000; i++ ) {
            while( ! sink.offer(i) ) {
                Thread.yield();
            }
        }
        sink.complete();

        System.out.println("received:" + cnt.get());
        Thread.sleep(1000);
        System.out.println("received:" + cnt.get());

        Assert.assertTrue(cnt.get() == 100_000);

        System.out.println("count:" + DispatcherThread.activeDispatchers.get());
    }

}

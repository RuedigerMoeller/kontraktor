package examples.rxjava;


import examples.MyEvent;
import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.RateMeasure;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 07/07/15.
 */
public class RxJava {

    public static final int NUM_MSG = 50_000_000;

    @Test
    public void rxToKontraktorTest() {
        Observable<Integer> range = Observable.range(0, NUM_MSG);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);
        AtomicInteger count = new AtomicInteger(0);

        RateMeasure rm = new RateMeasure("events");
        Promise<Integer> finished = new Promise<>();
        KxReactiveStreams.get().asKxPublisher(pub)
            .subscribe((r, e) -> {
                rm.count();
                if (Actors.isResult(e)) {
                    count.incrementAndGet();
                } else {
                    finished.resolve(count.get());
                }
            });

        Assert.assertTrue(finished.await(50000).intValue() == NUM_MSG);
    }

    @Test
    public void rxToJ8StreamTest() {
        Observable<Integer> range = Observable.range(0, NUM_MSG/4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);
        AtomicInteger count = new AtomicInteger(0);

        RateMeasure rm = new RateMeasure("events");
        Promise<Integer> finished = new Promise<>();
        KxReactiveStreams.get().asKxPublisher(pub)
            .async()
            .stream(stream -> {
                stream.forEach(i -> {
                    rm.count();
                    count.incrementAndGet();
                });
                finished.resolve(count.get());
            });

        Assert.assertTrue(finished.await(50000).intValue() == NUM_MSG/4);
    }

    @Test
    public void rxToIteratorTest() {
        Observable<Integer> range = Observable.range(0, NUM_MSG/4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);
        AtomicInteger count = new AtomicInteger(0);

        RateMeasure rm = new RateMeasure("events");
        Promise<Integer> finished = new Promise<>();
        KxReactiveStreams.get().asKxPublisher(pub)
            .async()
            .iterator(it -> {
                while (it.hasNext()) {
                    rm.count();
                    count.incrementAndGet();
                }
                finished.resolve(count.get());
            });

        Assert.assertTrue(finished.await(50000).intValue() == NUM_MSG/4);
    }

    @Test
    public void remotingTest() {
        Observable<Integer> range = Observable.range(0, NUM_MSG/4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asKxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3456));

        RateMeasure rm = new RateMeasure("events");
        AtomicInteger cnt = new AtomicInteger(0);
        Promise<Integer> finished = new Promise<>();
        KxReactiveStreams.get()
            .connect(Integer.class, new TCPConnectable().host("localhost").port(3456))
            .subscribe((r, e) -> {
                rm.count();
                if (Actors.isResult(e))
                    cnt.incrementAndGet();
                else
                    finished.resolve(cnt.get());
            });
        Assert.assertTrue(finished.await(50000) == NUM_MSG/4);
    }

    @Test
    public void remotingToJ8Streams() {
        Observable<Integer> range = Observable.range(0, NUM_MSG / 4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asKxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3457));

        RateMeasure rm = new RateMeasure("events");
        Promise<Integer> finished = new Promise<>();
        KxReactiveStreams.get()
            .connect(Integer.class, new TCPConnectable().host("localhost").port(3457))
            .stream(stream -> {
                long count =
                    stream
                        .map(i -> {
                            rm.count();
                            return i;
                        })
                        .count();
                finished.resolve((int) count);
            });
        Assert.assertTrue(finished.await(50000) == NUM_MSG / 4);
    }

    @Test
    public void remotingRxToRx() throws InterruptedException {
        Observable<Integer> range = Observable.range(0, NUM_MSG / 4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asKxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3458));

        RateMeasure rm = new RateMeasure("events");

        KxPublisher<Integer> remoteStream =
            KxReactiveStreams.get()
                .connect(Integer.class, new TCPConnectable().host("localhost").port(3458));

        CountDownLatch cnt = new CountDownLatch(NUM_MSG/4);
        RxReactiveStreams.toObservable(remoteStream)
            .forEach(i -> {
                rm.count();
                cnt.countDown();
            });

        cnt.await(50, TimeUnit.SECONDS);

        Assert.assertTrue(cnt.getCount() == 0);
    }

    @Test
    public void remotingRxToRxWebSocket() throws InterruptedException {
        Observable<Integer> range = Observable.range(0, NUM_MSG/4);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(7777).urlPath("/ws/rx"));

        RateMeasure rm = new RateMeasure("events");

        KxPublisher<Integer> remoteStream =
            KxReactiveStreams.get()
                .connect(Integer.class, new WebSocketConnectable().url("ws://localhost:7777/ws/rx"));

        CountDownLatch cnt = new CountDownLatch(NUM_MSG/4);
        RxReactiveStreams.toObservable(remoteStream)
            .forEach(i -> {
                rm.count();
                cnt.countDown();
            });

        cnt.await(50, TimeUnit.SECONDS);

        Assert.assertTrue(cnt.getCount() == 0);
    }

    @Test
    public void remotingRxToRxWebSocketSampleEvent() throws InterruptedException {
        Observable<Integer> range = Observable.range(0, NUM_MSG/4);
        Publisher<MyEvent> pub = RxReactiveStreams.toPublisher(range.map( i -> new MyEvent(i, Math.random(), "Hello"+i) ));

        KxReactiveStreams.get().asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(7778).urlPath("/ws/rx"));

        RateMeasure rm = new RateMeasure("events");

        KxPublisher<MyEvent> remoteStream =
            KxReactiveStreams.get()
                .connect(MyEvent.class, new WebSocketConnectable().url("ws://localhost:7778/ws/rx"));

        CountDownLatch cnt = new CountDownLatch(NUM_MSG/4);
        RxReactiveStreams.toObservable(remoteStream)
            .forEach(i -> {
                rm.count();
                cnt.countDown();
            });

        cnt.await(50, TimeUnit.SECONDS);

        Assert.assertTrue(cnt.getCount() == 0);
    }


}

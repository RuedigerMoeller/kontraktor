package examples.akka;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.impl.PublisherSink;
import akka.stream.impl.SubscriberSink;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;
import org.reactivestreams.Publisher;
import rx.RxReactiveStreams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 12/07/15.
 */
public class AkkaInterop {


    @Test
    public void akkaAkka() throws InterruptedException {
        final int NUM_MSG = 10_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        // Attention: buffer + batchsizes of Akka need increase in order to get performance
        final ActorMaterializer mat =
            ActorMaterializer.create(
                ActorMaterializerSettings.create(system)
                    .withInputBuffer(4096, 4096), system
            );

        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);
        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();

        Source.from(it)
            .runWith(Sink.foreach(elem -> {
                rm.count();
                count.countDown();
            }), mat);

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+count.getCount());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test
    public void akkaConsumes() throws InterruptedException {
        final int NUM_MSG = 20_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        // Attention: buffer + batchsizes of Akka need increase in order to get performance
        final ActorMaterializer mat =
            ActorMaterializer.create(
                ActorMaterializerSettings.create(system)
                    .withInputBuffer(4096, 4096), system
            );

        KxReactiveStreams kxStreams = KxReactiveStreams.get();
        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Source.from(kxStreams.produce(IntStream.range(0, NUM_MSG)))
            .runWith(Sink.foreach( elem -> {
                rm.count();
                count.countDown();
            }), mat);

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+count.getCount());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test
    public void kontraktorConsumes() throws InterruptedException {
        final int NUM_MSG = 50_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        final Materializer mat = ActorMaterializer.create(system);

        KxReactiveStreams kxStreams = KxReactiveStreams.get();
        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();
        Publisher<Integer> pub = (Publisher<Integer>) Source.from(it).runWith(Sink.publisher(), mat);

        kxStreams.asKxPublisher(pub)
            .subscribe((res, err) -> {
                if (Actors.isResult(err)) {
                    rm.count();
                    count.countDown();
                }
            });

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+count.getCount());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test
    public void serveAkka_RemoteToKontraktor() throws InterruptedException {
        final int NUM_MSG = 50_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        final Materializer mat = ActorMaterializer.create(system);

        KxReactiveStreams kxStreams = KxReactiveStreams.get();
        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();
        Publisher<Integer> pub = (Publisher<Integer>) Source.from(it).runWith(Sink.publisher(), mat);

        kxStreams.asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(6790).urlPath("akka"));

        // subscribe with kontraktor client
        new KxReactiveStreams(true)
            .connect(Integer.class, new WebSocketConnectable().url("ws://localhost:6790/akka"))
            .subscribe((res, err) -> {
                if (Actors.isResult(err)) {
                    rm.count();
                    count.countDown();
                }
            });

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+count.getCount());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test @Ignore
    public void serveAkkaKontraktorAsBridge() throws InterruptedException {

        Log.setLevel(Log.DEBUG);

        final int NUM_MSG = 20_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        // Attention: buffer + batchsizes of Akka need increase in order to get performance
        final ActorMaterializer mat =
            ActorMaterializer.create(
                ActorMaterializerSettings.create(system)
                    .withInputBuffer(8192, 8192), system
            );

        KxReactiveStreams kxStreams = KxReactiveStreams.get();
        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();
        Publisher<Integer> pub = (Publisher<Integer>) Source.from(it).runWith(Sink.publisher(), mat);

        kxStreams.asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(6789).urlPath("akka"));

        AtomicInteger kontraktorCount = new AtomicInteger(0);
        // subscribe with akka client
        KxPublisher<Integer> remotedPublisher =
            new KxReactiveStreams(true).connect(Integer.class, new WebSocketConnectable().url("ws://localhost:6789/akka"))
//                .async(); // important: insert kontraktor endpoint as akka streams seems not tuned for network level latency
                .map(x -> { // (*)
                    kontraktorCount.incrementAndGet();
                    return x;
                });

        // for unknown reasons, this loses messages .. kontraktor receiver (*) above gets them all.
        // when trying to reproduce this locally the message loss seems not to occur.
        // as one can see from the counters the message loss is linear (not related to stream completion)
        // I think its an Akka issue as same test works if consuming from RxJava (see below)
        Source.from(remotedPublisher)
            .runWith(Sink.foreach(elem -> {
                rm.count();
                count.countDown();
            }), mat);

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+(NUM_MSG-count.getCount())+" kontraktor count:"+kontraktorCount.get());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test
    public void same_as_serveAkkaKontraktorAsBridge_use_RxJavaAtReceiverSide() throws InterruptedException {

        Log.setLevel(Log.DEBUG);

        final int NUM_MSG = 20_000_000;
        final ActorSystem system = ActorSystem.create("reactive-interop");
        // Attention: buffer + batchsizes of Akka need increase in order to get performance
        final ActorMaterializer mat =
            ActorMaterializer.create(
                ActorMaterializerSettings.create(system)
                    .withInputBuffer(8192, 8192), system
            );

        KxReactiveStreams kxStreams = KxReactiveStreams.get();
        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();
        Publisher<Integer> pub = (Publisher<Integer>) Source.from(it).runWith(Sink.publisher(), mat);

        kxStreams.asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(6789).urlPath("akka"));

        AtomicInteger kontraktorCount = new AtomicInteger(0);
        // subscribe with akka client
        KxPublisher<Integer> remotedPublisher =
            new KxReactiveStreams(true).connect(Integer.class, new WebSocketConnectable().url("ws://localhost:6789/akka"))
//                .async(); // important: insert kontraktor endpoint as akka streams seems not tuned for network level latency
                .map(x -> { // (*)
                    kontraktorCount.incrementAndGet();
                    return x;
                });

        RxReactiveStreams.toObservable(remotedPublisher)
            .forEach(i -> {
                rm.count();
                count.countDown();
            });


        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+(NUM_MSG-count.getCount())+" kontraktor count:"+kontraktorCount.get());
            Thread.sleep(1000);
        }

        system.shutdown();
        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff
    }

    @Test
    public void kontraktorRemoteKontraktor() throws InterruptedException {

        Log.setLevel(Log.DEBUG);

        final int NUM_MSG = 20_000_000;

        KxReactiveStreams kxStreams = KxReactiveStreams.get();

        RateMeasure rm = new RateMeasure("rate");
        CountDownLatch count = new CountDownLatch(NUM_MSG);

        Iterable it = () -> IntStream.range(0, NUM_MSG).mapToObj(x -> x).iterator();
        Publisher<Integer> pub = kxStreams.produce(it.iterator());

        kxStreams.asKxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(6789).urlPath("not_akka"));

        // subscribe with akka client
        KxPublisher<Integer> remotedPublisher =
            new KxReactiveStreams(true).connect(Integer.class, new WebSocketConnectable().url("ws://localhost:6789/not_akka"));

        remotedPublisher
            .subscribe((res, err) -> {
                if (Actors.isResult(err)) {
                    rm.count();
                    count.countDown();
                }
            });

        int secondsWait = 50;
        while( count.getCount() > 0 && secondsWait-- > 0 ) {
            System.out.println("count:"+count.getCount());
            Thread.sleep(1000);
        }

        Assert.assertTrue(count.getCount() == 0);
        Thread.sleep(1000); // give time closing stuff

    }

}

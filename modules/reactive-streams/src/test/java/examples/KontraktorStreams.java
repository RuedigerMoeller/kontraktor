package examples;

import jdk.nashorn.internal.ir.annotations.Ignore;
import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.Date;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 09/07/15.
 */
public class KontraktorStreams {

    public static final int NUM_MSG = 20_000_000;

    public static class Server extends Actor<Server> {
        EventSink<Integer> counter;
        EventSink<Date> date;

        KxPublisher counterPub, datePub; // required to remote

        int count = 0;

        public void init() {
            Thread.currentThread().setName("Server Actor");
            counter = new EventSink();
            date = new EventSink();

            counterPub = counter.map(x -> x);
            datePub = date.map(x -> x);

            doTime();
            doCounter();
        }

        public void doTime() {
            if ( ! isStopped() ) {
                date.offer(new Date());
                delayed(1000, () -> doTime());
            }
        }

        public void doCounter() {
            if ( ! isStopped() ) {
                if ( counter.offer(count) ) {
                    count++;
                }
                delayed(500, () -> doCounter());
            }
        }

        public IPromise<KxPublisher<Date>> getDate() {
            return new Promise<>(datePub);
        }

        public IPromise<KxPublisher<Integer>> getCounter() {
            return new Promise<>(counterPub);
        }

        public IPromise<KxPublisher<String>> getCounterAsString() {
            // .syncMap is not remotable
            // (actually eventSink.syncMap( i -> string ).map(x->x) should be used here for efficiency)
            return new Promise<>(counterPub.map(i -> "string:" + i));
        }

        public IPromise<KxPublisher<Integer>> countFast( int max ) {
            return new Promise<>(KxReactiveStreams.get().produce(IntStream.range(0,max)));
        }

    }

    public static class Client extends Actor<Client> {

        Server serv;
        public void init() {
            serv = (Server) new TCPConnectable(Server.class,"localhost", 9876).connect().await();
            Thread.currentThread().setName("Client Actor");
        }

        public IPromise<Integer> goFast() {
            int count[] = {0}; // we are single threaded
            Promise terminationPromise = new Promise();
            RateMeasure ms = new RateMeasure("rate");
            serv.countFast(NUM_MSG).await()
                .subscribe((r, e) -> { // FIXME: Consider SubscriberAdapter
                    if (isResult(e)) {
                        ms.count();
                        count[0]++;
                    } else {
                        System.out.println("finished goFast()");
                        terminationPromise.resolve(count[0]);
                    }
                });
            return terminationPromise;
        }

        public IPromise<Integer> goFastWithStreamsInActor() {
            Promise terminationPromise = new Promise();
            RateMeasure ms = new RateMeasure("rate");
            serv.countFast(NUM_MSG).await()
                .stream(stream -> {
                    // note: we are inside actor, this runs inside actor thread
                    int cnt[] = {0};
                    stream.forEach(i -> {
                        ms.count();
                        cnt[0]++;
                    });
                    terminationPromise.resolve(cnt[0]);
                });
            return terminationPromise;
        }

        public IPromise<Integer> goFastWithIteratorInActor() {
            Promise terminationPromise = new Promise();
            RateMeasure ms = new RateMeasure("rate");
            serv.countFast(NUM_MSG).await()
                .iterator( iterator -> {
                    // note: we are inside actor, this runs inside actor thread
                    int cnt[] = {0};
                    while (iterator.hasNext()) {
                        Integer next = iterator.next();
                        ms.count();
                        cnt[0]++;
                    }
                    terminationPromise.resolve(cnt[0]);
                });
            return terminationPromise;
        }
    }

    Server server;
    public void initServer() {
        if ( server == null ) {
            server = Actors.AsActor(Server.class,KxReactiveStreams.DEFQSIZE); // need fat queue !!!
            server.init();
            new TCPNIOPublisher(server,9876).publish( actor -> {
                System.out.println("disconnected !");
            }).await();
        }
    }

    @Test @Ignore // util to run server separate
    public void runServer() throws InterruptedException {
        Log.setLevel(Log.DEBUG);
        initServer();
        Thread.sleep(10000000l);
    }

    @Test @Ignore // util to start client in separate process
    public void singleClientNoServer() throws InterruptedException {
        Client client = Actors.AsActor(Client.class);
        client.init();
        Log.setLevel(Log.DEBUG);

        Integer result = client.goFastWithStreamsInActor().await(60_000);
        
        Assert.assertTrue(result.intValue() == NUM_MSG);

        Integer res1 = client.goFast().await(60_000);
        Assert.assertTrue(res1.intValue() == NUM_MSG);

        Integer res2 = client.goFastWithIteratorInActor().await(60_000);
        Assert.assertTrue( res2.intValue() == NUM_MSG );

    }

    @Test
    public void singleClient() throws InterruptedException {
        initServer();
        singleClientNoServer();
    }
}

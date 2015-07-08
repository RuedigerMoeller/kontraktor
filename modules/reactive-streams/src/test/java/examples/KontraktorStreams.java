package examples;

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
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.Date;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 09/07/15.
 */
public class KontraktorStreams {

    public static class Server extends Actor<Server> {
        EventSink<Integer> counter;
        EventSink<Date> date;

        KxPublisher counterPub, datePub; // required to remote

        int count = 0;

        public void init() {
            counter = new EventSink();
            date = new EventSink();

            counterPub = counter.asyncMap( x -> x );
            datePub = date.asyncMap( x -> x );

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
            // .map is not remotable
            // (actually eventSink.map( i -> string ).asyncMap(x->x) should be used here for efficiency)
            return new Promise<>(counterPub.asyncMap( i -> "string:"+i));
        }

        public IPromise<KxPublisher<Integer>> countFast( int max ) {
            return new Promise<>(KxReactiveStreams.get().produce(IntStream.range(0,max)));
        }

    }

    public static class Client extends Actor<Client> {

        Server serv;
        public void init() {
            serv = (Server) new TCPConnectable(Server.class,"localhost", 9876).connect().await();
        }

        public IPromise goFast() {
            Promise terminationPromise = new Promise();
            RateMeasure ms = new RateMeasure("rate");
            serv.countFast(20_000_000).await()
                .subscribe( (r,e) -> { // FIXME: Consider SubscriberAdapter
                    if ( isResult(e) ) {
                        ms.count();
                    } else {
                        System.out.println("finished goFast()");
                        terminationPromise.complete();
                    }
                });
            return terminationPromise;
        }

        public IPromise goFastWithStreamsInActor() {
            Promise terminationPromise = new Promise();
            RateMeasure ms = new RateMeasure("rate");
            serv.countFast(20_000_000).await()
                .stream( stream -> {
                    stream.forEach(i -> ms.count());
                    terminationPromise.complete();
                });
            return terminationPromise;
        }

    }


    Server server;
    public void initServer() {
        if ( server == null ) {
            server = Actors.AsActor(Server.class,KxReactiveStreams.DEFAULTQSIZE); // need fat queue !!!
            server.init();
            new TCPNIOPublisher(server,9876).publish( actor -> {
                System.out.println("disconnected !");
            }).await();
        }
    }

    @Test
    public void singleClient() throws InterruptedException {
        initServer();
        Client client = Actors.AsActor(Client.class);
        client.init();
//        client.goFast().await(60_000);
        client.goFastWithStreamsInActor().await(60_000);
    }
}

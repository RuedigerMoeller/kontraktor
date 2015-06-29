package kstreams;

import org.junit.Test;

import static org.nustaq.kontraktor.reactivestreams.ReaktiveStreams.*;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.PublisherActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 28/06/15.
 */
public class KStreamsTest {


    @Test
    public void testServer() throws InterruptedException {

        AtomicLong counter = new AtomicLong(0);

        EventSink<String> stringStream = new EventSink<>();

        Processor<String,String> fin = stringStream.map(in -> in + in, 1000);
        get().newPublisherServer(fin, new TCPNIOPublisher().port(7777), null).await();

//        AtomicLong received1 = new AtomicLong(0);
//        fin.subscribe(
//            subscriber(500, (str, err) -> {
//                if (isFinal(err)) {
//                    System.out.println("complete");
//                } else if (isError(err)) {
//                    System.out.println("ERROR");
//                } else {
//                    received1.incrementAndGet();
//                    LockSupport.parkNanos(1000 * 2000); // simulate slow receiver
//                }
//            }
//        ));

//        if ( System.currentTimeMillis() == 0 )
        {
            long l = System.currentTimeMillis();
            while( true ) {
                if ( stringStream.offer(""+counter.get()) ) {
                    counter.incrementAndGet();
                }
                if ( System.currentTimeMillis()-l > 1000 ) {
                    System.out.println("sent:"+counter.get());
                    l = System.currentTimeMillis();
                }
            }
        }

    }

    @Test
    public void testClient() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        Publisher<String> remote = get().connectRemotePublisher(String.class, new TCPConnectable().host("localhost").port(7777), null).await();
        ((PublisherActor)remote).hallo("POK");
        remote.subscribe(
            subscriber(1000, (str, err) -> {
                if (isFinal(err)) {
                    System.out.println("complete");
                } else if (isError(err)) {
                    System.out.println("ERROR");
                } else {
                    received.incrementAndGet();
                    LockSupport.parkNanos(1000 * 1000); // simulate slow receiver
                }
            }
        ));
//        if ( System.currentTimeMillis() == 0 )
        {
            long l = System.currentTimeMillis();
            long prev = 0;
            while( true ) {
                Thread.sleep(10);
                if ( System.currentTimeMillis()-l > 1000 ) {
                    long rate = received.get() - prev;
                    prev = received.get();
                    System.out.println("received:"+received.get());
                    System.out.println("      "+rate);
                    l = System.currentTimeMillis();
                }
            }
        }
    }

}

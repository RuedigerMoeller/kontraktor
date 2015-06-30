package kstreams;

import org.junit.Test;

import static org.nustaq.kontraktor.reactivestreams.ReaktiveStreams.*;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.PublisherActor;
import org.nustaq.kontraktor.remoting.tcp.*;
import org.nustaq.kontraktor.util.*;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 28/06/15.
 */
public class KStreamsTest {

    @Test
    public void simpleTest() {

        AtomicLong counter = new AtomicLong(0);
        AtomicLong received = new AtomicLong(0);

        EventSink<String> stringStream = new EventSink<>();

        Processor<String,String> fin = stringStream.map(in -> in + in);

//        get().newPublisherServer(fin, new TCPNIOPublisher().port(7777), null).await();

        fin.subscribe(
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

        long l = System.currentTimeMillis();
        long prev = 0;
        while( true ) {
            if ( stringStream.offer(""+counter.get()) ) {
                counter.incrementAndGet();
            }
            if ( System.currentTimeMillis()-l > 1000 ) {
                long rate = received.get() - prev;
                prev = received.get();
                System.out.println("sent:"+counter.get()+" received:"+received.get()+" diff:"+(counter.get()-received.get()));
                System.out.println("      "+rate);
                l = System.currentTimeMillis();
            }
        }


    }

    @Test
    public void testServer() throws InterruptedException {

        AtomicLong counter = new AtomicLong(0);

        EventSink<String> stringStream = new EventSink<>();

//        Processor<String,String> fin = stringStream.map(in -> in + in);
//        get().newPublisherServer(stringStream, new TCPNIOPublisher().port(7777), actor -> {
//            System.out.println("disconnect of "+actor);
//        }).await();

        get().newPublisherServer(stringStream, new TCPNIOPublisher().port(7777), actor -> {
            System.out.println("disconnect of "+actor);
        }).await();

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
            int prev = 0;
            long l = System.currentTimeMillis();
            while( true ) {
                long cn = counter.get();
                if ( stringStream.offer(""+ cn) ) {
                    counter.incrementAndGet();
                }
                if ( System.currentTimeMillis()-l > 1000 ) {
                    System.out.println("sent:"+ cn);
                    System.out.println("    :"+(cn -prev));
                    prev = (int) cn;
                    l = System.currentTimeMillis();
                }
            }
        }

    }

    @Test
    public void testClient() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        Publisher<String> remote = get().connectRemotePublisher(String.class, new TCPConnectable().host("localhost").port(7777), null).await();
        RateMeasure ms = new RateMeasure("event rate");
        remote.subscribe(
            subscriber( (str, err) -> {
                if (isFinal(err)) {
                    System.out.println("complete");
                } else if (isError(err)) {
                    System.out.println("ERROR");
                } else {
                    received.incrementAndGet();
                    ms.count();
//                    LockSupport.parkNanos(1000 * 1000); // simulate slow receiver
                }
            }
        ));
        Thread.sleep(1000000);
    }

}

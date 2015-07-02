package kstreams;

import org.junit.Test;

import static org.nustaq.kontraktor.reactivestreams.ReaktiveStreams.*;

import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KPublisher;
import org.nustaq.kontraktor.remoting.tcp.*;
import org.nustaq.kontraktor.util.*;

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

        EventSink<String> stringSink = new EventSink<>();

        stringSink
//            .map(in -> in + " " + in)
            .map(in -> in.length())
            .subscribe( (str, err) -> {
                if (isErrorOrComplete(err)) {
                  System.out.println("complete");
                } else if (isError(err)) {
                  System.out.println("ERROR");
                } else {
                  received.incrementAndGet();
                  LockSupport.parkNanos(1000 * 1000); // simulate slow receiver
                }
            });

        long l = System.currentTimeMillis();
        long prev = 0;
        while( true ) {
            if ( stringSink.offer(""+counter.get()) ) {
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

        stringStream.publish(new TCPPublisher().port(7777), actor -> {
            System.out.println("disconnect of " + actor);
        });

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

    @Test
    public void testClient() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        KPublisher<String> remote = get().connectRemotePublisher(String.class, new TCPConnectable().host("localhost").port(7777), null).await();
        RateMeasure ms = new RateMeasure("event rate");
        remote
            .subscribe(
                (str, err) -> {
                    if (isErrorOrComplete(err)) {
                        System.out.println("complete e:"+err+" r:"+str);
                    } else if (isError(err)) {
                        System.out.println("ERROR "+err);
                    } else {
                        received.incrementAndGet();
                        ms.count();
                    }
                });
        while( true ) {
            Thread.sleep(100);
        }
    }

    @Test
    public void testClient1() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        KPublisher<String> remote = get().connectRemotePublisher(String.class, new TCPConnectable().host("localhost").port(7777), null).await();
        RateMeasure ms = new RateMeasure("event rate");
        remote
            .map(string -> string.length())
            .map(number -> number > 10 ? number : number )
            .subscribe(
                (str, err) -> {
                    if (isErrorOrComplete(err)) {
                        System.out.println("complete");
                    } else if (isError(err)) {
                        System.out.println("ERROR");
                    } else {
                        received.incrementAndGet();
                        ms.count();
                    }
                });
        while( true ) {
            Thread.sleep(100);
        }
    }

    @Test // slowdown
    public void testClient2() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        KPublisher<String> remote = get().connectRemotePublisher(String.class, new TCPConnectable().host("localhost").port(7777), null).await();
        RateMeasure ms = new RateMeasure("event rate");
        remote
            .map(string -> string.length())
            .map(number -> number > 10 ? number : number )
            .subscribe(
                (str, err) -> {
                    if (isErrorOrComplete(err)) {
                        System.out.println("complete");
                    } else if (isError(err)) {
                        System.out.println("ERROR");
                    } else {
                        received.incrementAndGet();
                        ms.count();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

        while( true ) {
            Thread.sleep(100);
        }

    }

}

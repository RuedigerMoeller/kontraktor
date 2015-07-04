package remotetreams;

import org.junit.Test;

import static org.nustaq.kontraktor.reactivestreams.ReaktiveStreams.*;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KPublisher;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.*;
import org.nustaq.kontraktor.util.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 28/06/15.
 *
 * note: this cannot be run like a normal unit test. The server has to be started, then client tests could be run.
 * I use it to test from within the ide only currently
 *
 */
public class TCPNIOKStreamsTest {

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

        stringStream.publish(getRemotePublisher(), actor -> {
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

    public ActorPublisher getRemotePublisher() {return new TCPPublisher().port(7777);}
    public ConnectableActor getRemoteConnector() {return new TCPConnectable().host("localhost").port(7777);}

    @Test
    public void testClient() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        Callback<ActorClientConnector> discon = (acc,err) -> {
            System.out.println("Client disconnected");
        };
        KPublisher<String> remote = get().connectRemotePublisher(String.class, getRemoteConnector(), discon).await();
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
        Callback<ActorClientConnector> discon = (acc,err) -> {
            System.out.println("Client disconnected");
        };
        KPublisher<String> remote = get().connectRemotePublisher(String.class, getRemoteConnector(), discon).await();
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
        Callback<ActorClientConnector> discon = (acc,err) -> {
            System.out.println("Client disconnected");
        };
        KPublisher<String> remote = get().connectRemotePublisher(String.class, getRemoteConnector(), discon).await();
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


    @Test // cleanup of async processors
    public void testClient3() throws InterruptedException {
        AtomicLong received = new AtomicLong(0);
        Callback<ActorClientConnector> discon = (acc,err) -> {
            System.out.println("Client disconnected");
        };
        KPublisher<String> remote = get().connectRemotePublisher(String.class, getRemoteConnector(), discon).await();
        RateMeasure ms = new RateMeasure("event rate");
        remote
            .asyncMap(string -> string.length())
            .asyncMap(number -> number > 10 ? number : number)
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

}

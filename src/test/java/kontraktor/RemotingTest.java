package kontraktor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
import org.nustaq.kontraktor.remoting.http.HttpObjectSocket;
import org.nustaq.kontraktor.remoting.http.UndertowHttpServerConnector;
import org.nustaq.kontraktor.remoting.tcp.NIOServerConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPServerConnector;
import org.nustaq.kontraktor.remoting.websockets.JSR356ServerConnector;
import org.nustaq.kontraktor.remoting.websockets.UndertowWebsocketServerConnector;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 10/05/15.
 */
public class RemotingTest {

    static AtomicInteger errors = new AtomicInteger();
    static boolean checkSequenceErrors = true;

    public static class RemotingTestService extends Actor<RemotingTestService> {

        public IPromise<String> promise(String s) {
            return new Promise<>(s+" "+s);
        }

        public void callback( Callback<String> cb ) {
            cb.stream("A");
            cb.stream("B");
            cb.stream("C");
            cb.finish();
        }

        public void spore( Spore<Integer,Integer> spore ) {
            spore.remote(1);
            spore.remote(2);
            spore.remote(3);
            spore.remote(4);
            spore.finish();
        }

        RateMeasure measure = new RateMeasure("calls",1000);
        public void benchMarkVoid(int someVal, String someString) {
            measure.count();
        }

        int prev = -1;
        public IPromise<Integer> benchMarkPromise(int someVal, String someString) {
            measure.count();
            if ( checkSequenceErrors && someVal != prev+1 ) {
                errors.incrementAndGet();
                System.out.println("error: received "+someVal+" curr:"+prev);
            }
            prev = someVal;
            return new Promise<>(someVal);
        }

    }

    // fixme: add connect-from-actor tests
    // fixme: add minbin tests
    // fixme: increase basic test coverage

    @Test @Ignore
    public void testWSJSR() throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = JSR356ServerConnector.Publish(service, "ws://localhost:8081/ws", null).await();
        RemotingTestService client = JSR356ClientConnector.Connect(RemotingTestService.class, "ws://localhost:8081/ws", null).await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }


    @Test
    public void testHttp() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runtHttp(coding);
    }

    @Test
    public void testHttpJson() throws Exception {
        Coding coding = new Coding(SerializerType.Json);
        runtHttp(coding);
    }

    public void runtHttp(Coding coding) throws InterruptedException {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowHttpServerConnector.Publish(service, "localhost", "/lp", 8082, coding).await();
//        RemotingTestService client = HttpClientConnector.Connect(RemotingTestService.class, "http://localhost:8082/lp", null, null, true, 1000).await(9999999);
        RemotingTestService client = HttpClientConnector.Connect(RemotingTestService.class, "http://localhost:8082/lp", null, null, coding, HttpClientConnector.LONG_POLL).await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testHttpMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowHttpServerConnector.Publish(service, "localhost", "/lp", 8082, null).await();
        RemotingTestService client = HttpClientConnector.Connect(RemotingTestService.class, "http://localhost:8082/lp", null, null, null).await(9999999);
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runWithClient(client, latch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testWS() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runWS(coding);
    }

    @Test
    public void testWSJson() throws Exception {
        Coding coding = new Coding(SerializerType.Json);
        runWS(coding);
    }

    public void runWS(Coding coding) throws InterruptedException {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowWebsocketServerConnector.Publish(service, "localhost", "/ws", 8081, coding).await();
        RemotingTestService client = JSR356ClientConnector.Connect(RemotingTestService.class, "ws://localhost:8081/ws", coding).await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testWSMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowWebsocketServerConnector.Publish(service, "localhost", "/ws", 8081 , null ).await();
        RemotingTestService client = JSR356ClientConnector.Connect(RemotingTestService.class, "ws://localhost:8081/ws", null).await(9999999);
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runWithClient(client, latch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testNIO() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runNio(coding);
    }

    @Test
    public void testNIOJSon() throws Exception {
        Coding coding = new Coding(SerializerType.Json);
        runNio(coding);
    }

    public void runNio(Coding coding) throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, coding).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch,coding);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testNIOMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testBlocking() throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch, null);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testBlockingMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    public void runnitTCP(CountDownLatch l, Coding coding) throws Exception {
        RemotingTestService client = (RemotingTestService) TCPClientConnector.Connect(RemotingTestService.class, "localhost", 8081, null, coding).await();
        runWithClient(client,l);
        Thread.sleep(2000); // wait for outstanding callbacks
        client.close();
    }

    protected void runWithClient(RemotingTestService client, CountDownLatch l) throws InterruptedException {
        Assert.assertTrue("Hello Hello".equals(client.promise("Hello").await(999999)));

        AtomicInteger replyCount = new AtomicInteger(0);
        ArrayList<Integer> sporeResult = new ArrayList<>();
        Promise sporeP = new Promise();
        client.spore(new Spore<Integer, Integer>() {
            @Override
            public void remote(Integer input) {
                stream(input);
            }
        }.forEach((res, e) -> {
            System.out.println("spore res " + res);
            sporeResult.add(res);
        }).onFinish(() -> {
            System.out.println("Finish");
            sporeP.complete();
        }));
        sporeP.await(30_000);
        Assert.assertTrue(sporeResult.size() == 4);

        System.out.println("one way performance");
        int numMsg = 15_000_000;
        for ( int i = 0; i < numMsg; i++ ) {
            client.benchMarkVoid(13, null);
        }
        System.out.println("two way performance");
        errors.set(0);
        boolean seq[] = new boolean[numMsg];
        for ( int i = 0; i < numMsg; i++ ) {
            if ( i%1_000_000==0 )
                System.out.println("sent "+i+" "+replyCount.get());
            while ( i - replyCount.get() > 200_000 ) { // FIXME: remoteref registry should do this, but how to handle unanswered requests ?
                LockSupport.parkNanos(1);
            }
            client.benchMarkPromise(i, null).then(s -> {
                replyCount.incrementAndGet();
                if (seq[s])
                    errors.incrementAndGet();
                seq[s] = true;
            });
        }
        Thread.sleep(2000);
        if (replyCount.get() != numMsg) {
            System.out.println("extend wait ..");
            Thread.sleep(HttpObjectSocket.LP_TIMEOUT*2);
        }
        for (int i = 0; i < seq.length; i++) {
            boolean b = seq[i];
            if ( !b ) {
                System.out.println("missing:"+i);
                errors.incrementAndGet();
            }
        }
        System.out.println("done "+Thread.currentThread()+" "+replyCount);
        junit.framework.Assert.assertTrue(replyCount.get() == numMsg);
        junit.framework.Assert.assertTrue(errors.get() == 0);
        l.countDown();
    }

}

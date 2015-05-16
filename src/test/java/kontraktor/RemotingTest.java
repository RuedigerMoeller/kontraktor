package kontraktor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
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

/**
 * Created by ruedi on 10/05/15.
 */
public class RemotingTest {

    public static class RemotingTestService extends Actor<RemotingTestService> {

        public IPromise<String> $promise(String s) {
            return new Promise<>(s+" "+s);
        }

        public void $callback( Callback<String> cb ) {
            cb.stream("A");
            cb.stream("B");
            cb.stream("C");
            cb.finish();
        }

        public void $spore( Spore<Integer,Integer> spore ) {
            spore.remote(1);
            spore.remote(2);
            spore.remote(3);
            spore.remote(4);
            spore.finish();
        }

        RateMeasure measure = new RateMeasure("calls",1000);
        public void $benchMarkVoid(int someVal, String someString) {
            measure.count();
        }

        public IPromise $benchMarkPromise(int someVal, String someString) {
            measure.count();
            return new Promise<>("ok");
        }

    }

    // fixme: add connect-from-actor tests
    // fixme: add minbin tests
    // fixme: increase basic test coverage

    @Test @Ignore
    public void testWSJSR() throws Exception {
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
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowHttpServerConnector.Publish(service, "localhost", "/lp", 8082, null).await();
        RemotingTestService client = HttpClientConnector.Connect(RemotingTestService.class, "http://localhost:8082/lp", null, null).await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testHttpMany() throws Exception {
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowHttpServerConnector.Publish(service, "localhost", "/lp", 8082, null).await();
        RemotingTestService client = HttpClientConnector.Connect(RemotingTestService.class, "http://localhost:8082/lp", null, null).await(9999999);
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
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = UndertowWebsocketServerConnector.Publish(service, "localhost", "/ws", 8081 , null ).await();
        RemotingTestService client = JSR356ClientConnector.Connect(RemotingTestService.class, "ws://localhost:8081/ws", null).await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testWSMany() throws Exception {
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
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testNIOMany() throws Exception {
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch);
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
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testBlockingMany() throws Exception {
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    public void runnitTCP(CountDownLatch l) throws Exception {
        RemotingTestService client = TCPClientConnector.Connect(RemotingTestService.class, "localhost", 8081, null, null).await();
        runWithClient(client,l);
        Thread.sleep(2000); // wait for outstanding callbacks
        client.$close();
    }

    protected void runWithClient(RemotingTestService client, CountDownLatch l) {
        Assert.assertTrue("Hello Hello".equals(client.$promise("Hello").await(999999)));

        ArrayList<Integer> sporeResult = new ArrayList<>();
        Promise sporeP = new Promise();
        client.$spore(new Spore<Integer, Integer>() {
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
        for ( int i = 0; i < 15_000_000; i++ ) {
            client.$benchMarkVoid(13, null);
        }
        System.out.println("two way performance");
        for ( int i = 0; i < 15_000_000; i++ ) {
            if ( i%1_000_000==0 )
                System.out.println("sent "+i);
            client.$benchMarkPromise(13, null);
//            LockSupport.parkNanos(1);
        }
        System.out.println("done "+Thread.currentThread());
        l.countDown();
    }

}

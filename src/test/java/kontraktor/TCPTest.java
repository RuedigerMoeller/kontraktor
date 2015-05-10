package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.tcp.NIOServerConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPServerConnector;
import org.nustaq.kontraktor.remoting.websockets.UndertowWebsocketServerConnector;
import org.nustaq.kontraktor.remoting.websockets.WebsocketAPIClientConnector;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 10/05/15.
 */
public class TCPTest {

    public static class TCPTestService extends Actor<TCPTestService> {

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

    @Test
    public void testWS() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorServer publisher = UndertowWebsocketServerConnector.Publish(service, "localhost", "/ws", 8081 , null ).await();
        TCPTestService client = WebsocketAPIClientConnector.Connect(TCPTestService.class,"ws://localhost:8081/ws",null).await(9999999);
        runnit(publisher,client);
        publisher.close();
    }

    @Test
    public void testNIO() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        runnitTCP(publisher);
        publisher.close();
    }

    @Test
    public void testNIOMany() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(publisher);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        exec.awaitTermination(1, TimeUnit.DAYS);
        publisher.close();
    }

    @Test
    public void testBlocking() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        runnitTCP(publisher);
        publisher.close();
    }

    @Test
    public void testBlockingMany() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(publisher);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        exec.awaitTermination(1, TimeUnit.DAYS);
        publisher.close();

    }

    public void runnitTCP(ActorServer publisher) throws Exception {
        TCPTestService client = TCPClientConnector.Connect(TCPTestService.class, "localhost", 8081, null).await();
        runWithClient(client);
        client.$close();
    }

    public void runnit(ActorServer publisher,TCPTestService client) throws Exception {
        runWithClient(client);
        client.$close();
    }

    protected void runWithClient(TCPTestService client) {
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
        sporeP.await();
        Assert.assertTrue(sporeResult.size() == 4);

        System.out.println("one way performance");
        for ( int i = 0; i < 10_000_000; i++ ) {
            client.$benchMarkVoid(13, null);
        }
        System.out.println("two way performance");
        for ( int i = 0; i < 10_000_000; i++ ) {
            if ( i%1_000_000==0 )
                System.out.println("sent "+i);
            client.$benchMarkPromise(13, null);
        }
        System.out.println("done "+Thread.currentThread());
    }

}

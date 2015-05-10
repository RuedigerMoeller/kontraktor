package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.tcp.NIOActorServer;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ruedi on 10/05/15.
 */
public class TCPTest {

    public static class TCPTestService extends Actor<TCPTestService> {

        public IPromise<String> $promise(String s) {
            return new Promise<>(s+" "+s);
        }

        public void $callback( long time, Callback<String> cb ) {
            cb.complete(new Date(time).toString(), null);
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

    @Test
    public void testNIO() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorPublisher publisher = NIOActorServer.Publish(service, 8081, null).await();
        runnit(publisher);
    }

    @Test
    public void testBlocking() throws Exception {
        TCPTestService service = Actors.AsActor(TCPTestService.class, 128000);
        ActorPublisher publisher = TCPActorServer.Publish(service, 8081, null).await();
        runnit(publisher);
    }

    public void runnit(ActorPublisher publisher) throws Exception {
        TCPTestService client = TCPActorClient.Connect(TCPTestService.class, "localhost", 8081).await();
        Assert.assertTrue("Hello Hello".equals(client.$promise("Hello").await()));

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
        for ( int i = 0; i < 4_000_000; i++ ) {
            client.$benchMarkVoid(13, null);
        }
        System.out.println("two way performance");
        for ( int i = 0; i < 20_000_000; i++ ) {
            if ( i%1_000_000==0 )
                System.out.println("sent "+i);
            client.$benchMarkPromise(13, null);
        }

        publisher.close();
    }

}

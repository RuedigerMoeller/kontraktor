package remoting;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

import java.util.concurrent.atomic.AtomicInteger;

public class RemotingTest {

    @Test
    public void test() {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);

        WebSocketPublisher pub = new WebSocketPublisher()
            .facade(serv)
            .hostName("0.0.0.0")
            .urlPath("/websocket")
            .port(7777)
            .serType(SerializerType.FSTSer);
        pub.publish().await();

        WebSocketConnectable con = new WebSocketConnectable()
            .actorClass(RemotingTA.class)
            .url("ws://localhost:7777/websocket");
        RemotingTA remote = (RemotingTA) con.connect().await();


        IPromise fin = new Promise();
        AtomicInteger count = new AtomicInteger();
        Integer expect = remote.sayHello(10, (str, err) -> {
            if (Actors.isCont(err)) {
                System.out.println("received:" + str);
                count.incrementAndGet();
            } else {
                fin.complete();
            }
        }).await();

        fin.await();

        Assert.assertTrue(expect.intValue() == count.intValue() );
    }
}

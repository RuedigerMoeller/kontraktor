package remoting;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

import java.util.concurrent.atomic.AtomicInteger;

import static org.nustaq.kontraktor.Actors.AsActor;

public class RemotingTest {

    @Test
    public void test() {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);

        // websocket
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
        fromRemote(con);

        // TCP NIO
        new TCPNIOPublisher(serv,7778).publish().await();
        fromRemote(new TCPConnectable(RemotingTA.class,"localhost",7778));

        // TCP Sync
        new TCPPublisher(serv,7780).publish().await();
        fromRemote(new TCPConnectable(RemotingTA.class,"localhost",7780));

        // Http-Longpoll
        new HttpPublisher(serv,"0.0.0.0","/httpapi",7779).publish().await();
        fromRemote(new HttpConnectable(RemotingTA.class,"http://localhost:7779/httpapi"));
    }

    @Ignore
    @Test public void testLimit() throws InterruptedException {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);
        // Http-Longpoll
        new HttpPublisher(serv,"0.0.0.0","/httpapi",7779).publish().await();

        RemotingTA client = (RemotingTA) new HttpConnectable(RemotingTA.class, "http://localhost:7779/httpapi").connect().await();
        for ( int i=0; i < 200; i++ ) {
            client.yes().then( (r,e)-> System.out.println(r+" "+e));
            Thread.sleep(10_000);
        }
    }

    private void fromRemote(ConnectableActor con) {
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

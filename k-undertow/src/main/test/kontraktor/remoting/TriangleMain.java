package kontraktor.remoting;

import junit.framework.Assert;
import kontraktor.remoting.helpers.WSServer;
import kontraktor.remoting.triangle.CenterActor;
import kontraktor.remoting.triangle.OuterActor;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServerAdapter;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.*;

/**
 * Created by ruedi on 13.08.2014.
 */
public class TriangleMain {

    @Test
    public void triangle() throws Exception {
        if (TCPTest.server != null) {
            TCPTest.server.closeConnection();
            TCPTest.server = null;
        }
        final TCPActorServerAdapter server = TCPActorServerAdapter.Publish(Actors.AsActor(CenterActor.class), 7778);
        CenterActor remoted = TCPActorClient.Connect(CenterActor.class, "localhost", 7778).await();
        doTest(remoted);


        server.closeConnection();
    }

    @Test
    public void triangleWS() throws Exception {
        if (TCPTest.server != null) {
            TCPTest.server.closeConnection();
            TCPTest.server = null;
        }
        CenterActor actor = Actors.AsActor(CenterActor.class);
        final ActorServerAdapter server = WSServer.getWebSocketActorServer(actor,8080);
        CenterActor remoted = WebSocketClient.Connect(CenterActor.class, "ws://localhost:8080/ws",null).await();
        doTest(remoted);

        server.closeConnection();
    }

    protected void doTest(CenterActor remoted) {
        // create client actors
        OuterActor outer[] = {
            Actors.AsActor(OuterActor.class),
            Actors.AsActor(OuterActor.class),
            Actors.AsActor(OuterActor.class)
        };

        for (int i = 0; i < outer.length; i++) {
            OuterActor outerActor = outer[i];
            outerActor.$init(i);
            remoted.$registerRemoteRef(i,outerActor);
        }

        System.out.println("..");

        AtomicInteger c = new AtomicInteger(0);
        Consumer<String> function = (r) -> {
            System.out.println(r);
            c.incrementAndGet();
        };
        remoted.$getOuter(0).await().$testCall("hu").then(function);
        remoted.$getOuter(1).await().$testCall("hu").then(function);
        remoted.$getOuter(2).await().$testCall("hu").then(function);

        for (int i = 0; i < outer.length; i++) {
            String await = remoted.$getOuter(i).await().$sendCall(remoted, (i + 1) % 3, "from " + i).await();
            System.out.println("->"+await);
            c.incrementAndGet();
        }

        stream(outer).forEach( o -> o.$close() );
        Assert.assertTrue(c.get() == 6);
    }

}

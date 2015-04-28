package kontraktor.remoting;

import junit.framework.Assert;
import kontraktor.remoting.helpers.MultiRefExposingActor;
import kontraktor.remoting.helpers.WSServer;
import org.junit.Test;
import static org.nustaq.kontraktor.Actors.*;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.RemoteConnection;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServerAdapter;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by ruedi on 03.04.2015.
 */
public class MultiRefTest {

    @Test
    public void multirefWS() throws Exception {
        Function server = act -> WSServer.getWebSocketActorServer((Actor)act,8080);
        Supplier client = () -> {
            try {
                return WebSocketClient.Connect(MultiRefExposingActor.class, "ws://localhost:8080/ws", null).await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };
        multiref(server,client);
    }

    @Test
    public void multirefTCP() throws Exception {
        Function server = act -> {
            try {
                return TCPActorServerAdapter.Publish((Actor) act, 7777);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };
        Supplier client = () -> {
            try {
                return TCPActorClient.Connect(MultiRefExposingActor.class, "localhost" ,7777, null).await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };
        multiref(server,client);
    }

    public void multiref(Function server, Supplier clientSup) throws Exception {
        MultiRefExposingActor multiRefExposingActor = Actors.AsActor(MultiRefExposingActor.class);
        Assert.assertTrue( !multiRefExposingActor.isRemote() );

        ActorServerAdapter srv = (ActorServerAdapter) server.apply(multiRefExposingActor);

        MultiRefExposingActor client1 = (MultiRefExposingActor) clientSup.get();

        Assert.assertTrue(!multiRefExposingActor.isRemote());

        MultiRefExposingActor.ServiceA sa1 = client1.createA().await();
        MultiRefExposingActor.ServiceA sa2 = client1.createA().await();
        MultiRefExposingActor.ServiceA sa3 = client1.createA().await();

        RemoteConnection con = sa2.__clientConnection;

        stream(sa1, sa2, sa3).forEach( sa -> sa.$endless() );

        Thread.sleep(4000);

        sa2.$fun().awaitPromise().then( (r,e) -> System.out.println( r + " " + e ) );

        System.out.println("stopping ..");
        sa2.$stopFromRemote();

        System.out.println("closing ..");
        sa2.$close();

        sa2.$fun().then((r, e) -> System.out.println(r + " " + e));

        Thread.sleep(1000);
        Assert.assertTrue(sa2.isStopped() && ((RemoteRefRegistry) con).getRemoteActors().size() == 3);

        sa1.$stopFromRemote();
        sa3.$stopFromRemote();

        Thread.sleep(1000);
        srv.closeConnection();
        Thread.sleep(1000);
    }
}

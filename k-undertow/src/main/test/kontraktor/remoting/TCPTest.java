package kontraktor.remoting;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ruedi on 31/03/15.
 */

public class TCPTest {

    static ActorServer server;

    public void setup() throws Exception {
        if ( server == null ) {
            server = ServerTestFacade.run();
        }
    }

    @Test
    public void bench() throws Exception {
        setup();
        int remoterefs = server.getConnections().size();
        ServerTestFacade run = createClientFacade(true);

        run.$close();
        Thread.sleep(1000);
        Assert.assertTrue(remoterefs == server.getConnections().size());
    }

    protected ServerTestFacade createClientFacade(boolean b) throws Exception {return ClientSideActor.run(b);}

    @Test
    public void manyfutures() throws Exception {
        setup();
        int remoterefs = server.getConnections().size();
        ServerTestFacade run = createClientFacade(false);

        server.getConnections().get(remoterefs);
        run.$close();
        Thread.sleep(1000);

        Assert.assertTrue(remoterefs == server.getConnections().size());
    }

    @Test
    public void basics() throws Exception {
        setup();
        ServerTestFacade run = createClientFac();

        Promise p = new Promise();
        p.timeoutIn(5000);
        long time = System.currentTimeMillis();
        run.$testCallWithCB(time, (r,e) -> {
            if (Actor.isFinal(e) ) {
                System.out.println("DONE "+r);
                p.resolve(r);
            } else {
                System.out.println("received:" + r);
            }
        });

        ArrayList<Integer> sporeResult = new ArrayList<>();
        Promise sporeP = new Promise();
        run.$sporeTest( new Spore<Integer,Integer>() {
            @Override
            public void remote(Integer input) {
                stream(input);
            }
        }.forEach( (res,e) -> {
            System.out.println("spore res "+res);
            sporeResult.add(res);
        }).onFinish( () -> {
            System.out.println("Finish");
            sporeP.complete();
        }));

        Assert.assertTrue(p.await().equals(new Date(time).toString()));
        Assert.assertTrue(run.$futureTest("A").await().equals("A A"));
        sporeP.await();
        Assert.assertTrue(sporeResult.size() == 4);

        run.$close();
        Thread.sleep(1000);
    }

    protected ServerTestFacade createClientFac() throws Exception {return TCPActorClient.Connect(ServerTestFacade.class, "localhost", 7777).await();}

}

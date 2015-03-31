package kontraktor.remoting;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.remoting.base.ActorServer;

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

        ActorServer.ActorServerConnection actorServerConnection = server.getConnections().get(remoterefs);
        int remoteRemoteActors = actorServerConnection.getRemoteActors().size();
        run.$close();
        Thread.sleep(1000);
        Assert.assertTrue(remoterefs == server.getConnections().size());
    }

}

package kontraktor.remoting;

import kontraktor.remoting.helpers.ClientSideActor;
import kontraktor.remoting.helpers.ServerTestFacade;
import org.junit.Test;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;

/**
 * Created by ruedi on 31/03/15.
 */
public class WSocketTest extends TCPTest {

    public void setup() throws Exception {
        if ( server == null ) {
            server = ServerTestFacade.runWS();
        }
    }

    protected ServerTestFacade createClientFacade(boolean b) throws Exception {
        return ClientSideActor.runWS(b);
    }
    protected ServerTestFacade createClientFac() throws Exception {
        return WebSocketClient.Connect(ServerTestFacade.class, "ws://localhost:8080/ws", null).await();
    }

    @Override @Test
    public void bench() throws Exception {
        super.bench();
    }

    @Override @Test
    public void manyfutures() throws Exception {
        super.manyfutures();
    }

    @Override @Test
    public void basics() throws Exception {
        super.basics();
    }

    @Override @Test
    public void ordering() throws Exception {
        super.ordering();
    }

    @Override @Test
    public void multiUse() throws Exception {
        super.multiUse();
    }
}

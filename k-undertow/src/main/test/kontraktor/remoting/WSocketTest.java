package kontraktor.remoting;

import org.junit.Test;

/**
 * Created by ruedi on 31/03/15.
 */
public class WSocketTest extends TCPTest {

    public void setup() throws Exception {
        if ( server == null ) {
            server = ServerTestFacade.runWS();
        }
    }

    protected ServerTestFacade createClientFacade(boolean b) throws Exception {return ClientSideActor.runWS(b);}

    @Override @Test
    public void bench() throws Exception {
        super.bench();
    }

    @Override @Test
    public void manyfutures() throws Exception {
        super.manyfutures();
    }
}

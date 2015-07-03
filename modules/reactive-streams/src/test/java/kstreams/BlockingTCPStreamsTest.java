package kstreams;

import org.junit.Test;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;

/**
 * Created by ruedi on 03/07/15.
 *
 * note: this cannot be run like a normal unit test. The server has to be started, then client tests could be run.
 * I use it to test from within the ide only currently
 *
*/
public class BlockingTCPStreamsTest extends TCPNIOKStreamsTest {

    @Override @Test
    public void testServer() throws InterruptedException {
        super.testServer();
    }

    @Override
    public ActorPublisher getRemotePublisher() {
        return new TCPPublisher().port(7777);
    }

    @Override @Test
    public void testClient() throws InterruptedException {
        super.testClient();
    }

    @Override @Test
    public void testClient1() throws InterruptedException {
        super.testClient1();
    }

    @Override @Test
    public void testClient2() throws InterruptedException {
        super.testClient2();
    }
}

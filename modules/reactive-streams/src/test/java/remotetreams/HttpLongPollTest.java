package remotetreams;

import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;

/**
 * Created by ruedi on 03/07/15.
 *
 * note: this cannot be run like a normal unit test. The server has to be started, then client tests could be run.
 * I use it to test from within the ide only currently
 *
 * Long Poll is not supported for reactive streams currently (systematical/technical issues)
 *
 */
public class HttpLongPollTest extends TCPNIOKStreamsTest {

    @Override @Test @Ignore
    public void testServer() throws InterruptedException {
        super.testServer();
    }

    @Override @Ignore
    public ActorPublisher getRemotePublisher() {
        return new HttpPublisher().hostName("localhost").port(8082).urlPath("/lp");
    }

    @Override
    public ConnectableActor getRemoteConnector() {
        return new HttpConnectable().url("http://localhost:8082/lp");
    }

    @Override @Test @Ignore
    public void testClient() throws InterruptedException {
        super.testClient();
    }

    @Override @Test @Ignore
    public void testClient1() throws InterruptedException {
        super.testClient1();
    }

    @Override @Test @Ignore
    public void testClient2() throws InterruptedException {
        super.testClient2();
    }
}

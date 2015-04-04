package kontraktor.remoting;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.undertow.KUndertowHttpServerAdapter;
import org.nustaq.kontraktor.undertow.Knode;

/**
 * Created by ruedi on 04.04.2015.
 */
public class HttpRemotingTest {

    public static class HttpTestService extends Actor<HttpTestService> {

        public void $test(String s) {
            System.out.println(s);
        }

        public void $hello( byte b, short s, int i, long l, char c, String str ) {
            System.out.println("byte "+b+", short "+s+", int "+i+", long "+l+", char "+c+", String "+str);
        }

        public void $helloBig( Byte b, Short s, Integer i, Long l, Character c, String str ) {
            System.out.println("byte "+b+", short "+s+", int "+i+", long "+l+", char "+c+", String "+str);
        }

    }

    @Test
    public void startServer() throws InterruptedException {
        int port = 8080;
        Knode knode = new Knode();
        knode.mainStub(new String[] {"-p",""+port});
        KUndertowHttpServerAdapter sAdapt = new KUndertowHttpServerAdapter(
            knode.getServer(),
            knode.getPathHandler()
        );

        RestActorServer restActorServer = new RestActorServer();
        restActorServer.joinServer("/api", sAdapt);

        HttpTestService service = Actors.AsActor(HttpTestService.class);
        restActorServer.publish("test",service);
        Thread.sleep(500000);
    }
}

package kontraktor;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.KontraktorSettings;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;

// cannot be run automatically, need to tweak code before running client
public class RemotingSecurityTest {

    public static class RServer extends Actor<RServer> {

//        static
        public void ShouldNotCallStaticMethods() {
            System.out.println("ShouldNotCallStaticMethods");
        }

//        @Local
        public void shouldNotCallLocalMethods() {
            System.out.println("shouldNotCallLocalMethods");
        }

        public IPromise allowed(String arg,Object x) {
            System.out.println("allowed "+arg);
            //throw new RuntimeException("an exception");
            return reject("error string");
        }

    }

    public void runServer() {
        RServer rs = Actors.AsActor(RServer.class);
        TCPPublisher publisher = new TCPPublisher()
            .port(4567)
            .facade(rs);
        ActorServer server = publisher.publish().await();
        System.out.println("server running");
    }

    public void runClient() {
        RServer client = (RServer) new TCPConnectable().host("localhost").port(4567).actorClass(RServer.class).connect().await();
//        client.ShouldNotCallStaticMethods();
        client.allowed("fail please", null ).then( (r,e) -> System.out.println("r:"+r+" e:"+e));
        try {
            Thread.sleep(100_000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        KontraktorSettings.FORWARD_EXCEPTIONS_TO_REMOTE = false;
        if ( args.length > 0 )
            new RemotingSecurityTest().runClient();
        else
            new RemotingSecurityTest().runServer();
    }
}

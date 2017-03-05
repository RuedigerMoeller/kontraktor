package kontraktor;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

/**
 * Created by ruedi on 05.03.17.
 */
public class RemotingPlayground {

    public static class AServ extends Actor<AServ> {

        public void pingme() {
            System.out.println("ping");
        }
    }

    public static void main(String[] args) {
        AServ server = Actors.AsActor(AServ.class);
        TCPNIOPublisher publisher = new TCPNIOPublisher(server, 5678);
        publisher.publish(actor -> System.out.println("actor " + actor + " disconnected")).await();

        AServ client = (AServ) new TCPConnectable(AServ.class,"localhost",5678).connect().await();
        client.pingme();
    }
}

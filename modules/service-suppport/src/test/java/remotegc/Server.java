package remotegc;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

import java.io.Serializable;

public class Server extends Actor<Server> {

    public void callbackMethod(Callback cb) {
        System.out.println("cbm called "+getConnections().size());
        getConnections().forEach( con -> System.out.println(con+" "+con.getOpenRemoteMappingsCount() ));
    }

    public IPromise getPromise() {
        System.out.println("prom called ");
        getConnections().forEach( con -> System.out.println(con+" "+con.getOpenRemoteMappingsCount() ));
        return new Promise();
    }

    public static void main(String[] args) {
        Server server = AsActor(Server.class);
        new TCPNIOPublisher().facade(server).port(7776).publish( a -> System.out.println("disconnect "+a));
    }
}

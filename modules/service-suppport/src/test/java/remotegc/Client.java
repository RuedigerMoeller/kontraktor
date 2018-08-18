package remotegc;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

public class Client extends Actor<Client> {

    public void run() {
        Server serv = (Server)new TCPConnectable()
            .actorClass(Server.class)
            .host("localhost").port(7776)
            .connect( (x,e) -> System.out.println("discon "+x), y -> System.out.println("oops "+y) ).await();
        serv.getPromise().then( (x,e) ->{
            System.out.println("will never receive something "+x+" "+e);
        });
        serv.callbackMethod( (xx,ee) -> {
            System.out.println("will never receive something except eot "+xx+" "+ee);
        });
        delayed( 1000, () -> {
            System.out.println(serv.__clientConnection);
        });
    }

    public static void main(String[] args) {
        Client client = AsActor(Client.class);
        client.run();
    }
}

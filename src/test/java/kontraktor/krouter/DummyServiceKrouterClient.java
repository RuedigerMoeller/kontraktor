package kontraktor.krouter;

import kontraktor.krouter.service.DummyService;
import kontraktor.krouter.service.ForeignClass;
import org.nustaq.kontraktor.AwaitException;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

public class DummyServiceKrouterClient {

    public static void main(String[] args) {

        DummyService tcpClient = (DummyService) new TCPConnectable()
            .host("localhost").port(6667)
            .actorClass(DummyService.class)
            .serType(SerializerType.JsonNoRef).connect( (x, err) -> System.exit(1) ).await();

        runTest(tcpClient);
    }

    private static void runTest(DummyService tcpClient) {
        try {
            tcpClient.ping().await(); // throws exception if not available
        } catch (AwaitException ae) {
            ae.printStackTrace();
            System.exit(1);
        }
        tcpClient.roundTrip(System.currentTimeMillis()).then( (l,e) -> {
            if ( l == null )
                System.out.println(e);
            else
                System.out.println("RT "+(System.currentTimeMillis()-(Long)l));
        });

        tcpClient.subscribe(new ForeignClass(1,2,3), (r, e) -> {
            System.out.println("subs "+r+" "+e);
        });
    }
}

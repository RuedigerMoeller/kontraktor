package kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.IOException;

/**
 * Created by ruedi on 09.08.14.
 */
public class ClientSideActor extends Actor<ClientSideActor> {

    public void $alsoHello( String x, int y ) {
        System.out.println("x:"+x+" y:"+y+" "+Thread.currentThread().getName());
    }

    public static class TA extends Actor<TA> {
        public void $run(ServerTestFacade test, ClientSideActor csa) {
            delayed(1000, () -> {
                test.$testCall("Hello", csa);
            });
            delayed(1000, () -> {
                test.$testCallWithCB(System.currentTimeMillis(), (r, e) -> {
                    System.out.println(r+" "+Thread.currentThread().getName());
                });
            });
            delayed(1000, () -> {
                test.$doubleMe("ToBeDoubled").then( (r,e) -> {
                    System.out.println(r+" "+Thread.currentThread().getName());
                    self().$run(test,csa);
                });
            });
        }
    }

    public static void main( String arg[] ) throws Exception {

        TCPActorClient.Connect(ServerTestFacade.class, "localhost", 7777).then( (test, err) -> {
            if (test != null) {
                ClientSideActor csa = Actors.AsActor(ClientSideActor.class);
                boolean bench = true;
                if (bench) {
                    while (true) {
                        // test.$benchMark(13, "this is a longish string");
                        test.$benchMark(13, null);
                    }
                } else {
                    TA t = Actors.AsActor(TA.class);
                    t.$run(test, csa);
                }
            }
        });

    }

}

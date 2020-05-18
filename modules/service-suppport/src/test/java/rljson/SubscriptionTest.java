package rljson;

import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.rlserver.RLJsonAuthResult;
import org.nustaq.kontraktor.services.rlserver.RLJsonServer;
import org.nustaq.kontraktor.services.rlserver.RLJsonSession;

public class SubscriptionTest {

    public static void main(String[] args) {
        BackOffStrategy.SLEEP_NANOS = 5 * 1000 * 1000; // 20 millis
        RLJsonServer server = (RLJsonServer) new TCPConnectable(RLJsonServer.class, "localhost", 7654)
            .connect()
            .await();
        server.ping().await();
        RLJsonAuthResult loginres = (RLJsonAuthResult) server.authenticate("honk", "hurz").await();
        RLJsonSession session = loginres.getSession();
        session.ping().await();

        session.subscribe("mySubscription_1","feed","subob.x == 77", (r,e) -> {
            if ( r != null ) {
                System.out.println("subs:"+r);
            }
        });
    }
}

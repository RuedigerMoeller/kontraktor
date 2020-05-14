package rljson;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.services.rlserver.RLJsonAuthResult;
import org.nustaq.kontraktor.services.rlserver.RLJsonServer;
import org.nustaq.kontraktor.services.rlserver.RLJsonSession;

import java.util.UUID;

import static org.nustaq.kontraktor.webapp.KJson.*;

public class TestRLJson {

    public static void main(String[] args) {
        RLJsonServer server = (RLJsonServer) new WebSocketConnectable(RLJsonServer.class, "ws://localhost:8087/ws")
            .coding( new Coding(SerializerType.JsonNoRef,RLJsonServer.CLAZZES) )
            .connect()
            .await();
        server.ping().await();
        RLJsonAuthResult loginres = (RLJsonAuthResult) server.authenticate("honk", "hurz").await();
        RLJsonSession session = loginres.getSession();
        session.ping().await();

        Object await = session.get("feed", "POKPOKPOK").await();
        System.out.println(await);

        session.update("feed",
            obj(
                "key", "POKPOKPOK",
                "array", arr(1,2,3,4,5),
                "subob", obj("x", 13, "random", Math.random() )
            ).toString()
        );

        for ( int i = 0; i < 100000; i++ ) {
            session.update("feed", obj(
                "key", "5ebd6f0a1637ffa8fa9cc5ef"+i,
                "array", arr(1,2,3,4,5),
                "subob", obj("x", i, "random", Math.random() )
            ).toString());
            if ( i%1000 == 0 && false ) { // mongodb slowdown
                try {
                    System.out.println("sleep "+i);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Object await1 = session.get("feed", "POKPOKPOK").await();
        System.out.println(await1);
    }
}

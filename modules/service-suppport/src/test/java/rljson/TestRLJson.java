package rljson;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.services.rlserver.RLJsonAuthResult;
import org.nustaq.kontraktor.services.rlserver.RLJsonServer;
import org.nustaq.kontraktor.services.rlserver.RLJsonSession;
import org.nustaq.kontraktor.util.PromiseLatch;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.nustaq.kontraktor.webapp.KJson.*;

public class TestRLJson {

    public static final int TIMEOUT_MILLIS = 5*60_000;

    public static void main(String[] args) {
        boolean QUERY = false;
        boolean UPSERT = false;
        int recordNum = 2_000_000;
//        int recordNum = 100_000;

//        RLJsonServer server = (RLJsonServer) new WebSocketConnectable(RLJsonServer.class, "ws://localhost:8087/ws")
//            .coding( new Coding(SerializerType.JsonNoRef,RLJsonServer.CLAZZES) )
//            .connect()
//            .await();
        RLJsonServer server = (RLJsonServer) new TCPConnectable(RLJsonServer.class, "localhost", 7654 )
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

        long tim = System.currentTimeMillis();

        if ( UPSERT ) {
            long timup = System.currentTimeMillis();
            for (int i = 0; i < recordNum; i++) {
                session.update("feed", obj(
                    "key", "5resbwttaopsdk37ff4a8fa9cc5ea" + i,
                    "array", arr(1, 2, 3, 4, 5),
                    "subob", obj("x", i, "random", Math.random())
                ).toString());
                if (i % 50_000 == 0) {
                    // rl native slowdown
                    System.out.println("waiting .. "+i);
                    session.get("feed", "POKPOKPOK").await(500_000);
                    System.out.println("done");

                    // mongodb slowdown
//                    try {
//                        System.out.println("sleep " + i);
//                        Thread.sleep(10000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            }
            session.get("feed", "POKPOKPOK").await(500_000);
            System.out.println("UPSERT TIME "+(System.currentTimeMillis()-timup));
        }

        if ( QUERY ) {
            AtomicInteger count = new AtomicInteger();
            session.select("feed", "true", (r,er) -> {
                if ( r != null )
                    count.incrementAndGet();
                else
                    System.out.println("query duration "+(System.currentTimeMillis()-tim)+" count "+count);
            });
        }

        Object await1 = session.get("feed", "POKPOKPOK").await(500_000);
        System.out.println("duration "+(System.currentTimeMillis()-tim));
        System.out.println(await1);

        runbenchquery(session,1).await(TIMEOUT_MILLIS);
        runbenchquery(session,2).await(TIMEOUT_MILLIS);
        runbenchquery(session,10).await(TIMEOUT_MILLIS);
        runbenchquery(session,50).await(TIMEOUT_MILLIS);
        runbenchquery(session,100).await(TIMEOUT_MILLIS);
    }

    private static IPromise runbenchquery(RLJsonSession session, int num) {
        System.out.println("************************* "+num+" *********************************");
        long qtim = System.currentTimeMillis();
        PromiseLatch pl = new PromiseLatch(num);
        for ( int i=0; i < num; i++ ) {
            int finalI = i;
            session.select("feed", "subob.x == 9999", (r, e) -> {
                if (r != null) {
//                    System.out.println("query " + r);
                }
                else {
                    long l = System.currentTimeMillis() - qtim;
                    System.out.println("querytime " + finalI + " " + l+ " pq:"+l/num);
                    pl.countDown();
                }
            });
        }
        return pl.getPromise();
    }
}

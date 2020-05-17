package rljson;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.rlserver.RLJsonAuthResult;
import org.nustaq.kontraktor.services.rlserver.RLJsonServer;
import org.nustaq.kontraktor.services.rlserver.RLJsonSession;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.query.Operator;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.nustaq.kontraktor.webapp.KJson.*;

public class TestRLJson {

    public static final int TIMEOUT_MILLIS = 5*60_000;

    public static void _main(String[] args) {
        QueryPredicate plain = new QueryPredicate("subob.x == 13 && field < 10");
        QueryPredicate plain1 = new QueryPredicate("subob.x == 13 && (field < 10 || key ** 'POK')");
        QueryPredicate klammer = new QueryPredicate("(a == 'x' && b == 'y') || ( z == 3 || o == 5)");
        QueryPredicate keiner = new QueryPredicate("y < 100 || (a == 'x' && b == 'y') && ( z == 3 && o == 5)");
        System.out.println("");
    }

    public static void main(String[] args) {
        BackOffStrategy.SLEEP_NANOS = 5 * 1000 * 1000; // 20 millis
        boolean QUERY = false;
        boolean UPSERT = false;
        int recordNum = 4_000_000;
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

//        while( server == null )
        {
            ArrayList hashR = new ArrayList();
            long hashTim = System.currentTimeMillis();
            Promise p = new Promise();
            session.selectHashed("feed",
                objs(
                    "subob.x", 88,
                    "/field", "13"
                ),
                "true",
                (r,e) -> {
                    if ( r != null ) {
                        hashR.add(r);
                    } else {
                        System.out.println("hashresult size "+hashR.size()+" "+(System.currentTimeMillis()-hashTim));
                        p.complete();
                    }
                });
            p.await();
        }

        ArrayList hashR1 = new ArrayList();
        long hashTim1 = System.currentTimeMillis();
        Promise p1 = new Promise();
        session.select("feed","subob.x == 88 && field == '13'", (r,e) -> {
            if ( r != null ) {
                hashR1.add(r);
            } else {
                System.out.println("no-hashresult size "+hashR1.size()+" "+(System.currentTimeMillis()-hashTim1));
                p1.complete();
            }
        });
        p1.await();

        long tim = System.currentTimeMillis();

        if ( UPSERT ) {
            long timup = System.currentTimeMillis();
            for (int i = 0; i < recordNum; i++) {
                session.update("feed", obj(
                    "key", "5reskopkodk37ff4a8fa9cc5ea" + i,
                    "array", arr(1, 2, 3, 4, 5),
                    "subob", obj("x", (int)(Math.random()*100), "random", Math.random()),
                    "field",""+(int)(Math.random()*100)
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
            Promise pp = new Promise();
            long timup = System.currentTimeMillis();
            AtomicInteger count = new AtomicInteger();
            session.select("feed", "subob.x < 1 && array ** 4", (r,er) -> {
                if ( r != null )
                    count.incrementAndGet();
                else {
                    System.out.println("query duration "+(System.currentTimeMillis()-tim)+" count "+count);
                    pp.complete();
                }
            });
            pp.await();
            System.out.println("QUERY TIME "+(System.currentTimeMillis()-timup));
        }

        if ( QUERY ) {
            runbenchquery(session, 1).await(TIMEOUT_MILLIS);
            runbenchquery(session, 2).await(TIMEOUT_MILLIS);
            runbenchquery(session, 10).await(TIMEOUT_MILLIS);
            runbenchquery(session, 50).await(TIMEOUT_MILLIS);
//            runbenchquery(session, 100).await(TIMEOUT_MILLIS);
        }

        System.out.println("-- done --");
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

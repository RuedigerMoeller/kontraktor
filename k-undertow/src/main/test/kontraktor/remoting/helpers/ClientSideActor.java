package kontraktor.remoting.helpers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 09.08.14.
 */
public class ClientSideActor extends Actor<ClientSideActor> {

    volatile static AtomicInteger error = new AtomicInteger(0);

    public void $alsoHello( String x, int y ) {
        System.out.println("x:"+x+" y:"+y+" "+Thread.currentThread().getName());
    }

    public static class TA extends Actor<TA> {
        public IPromise $runTest(ServerTestFacade test, ClientSideActor csa, int count) {
            Promise p0 = new Promise();
            Promise p1 = new Promise();
            Promise p2 = new Promise();
            delayed(1000, () -> {
                test.$testCall("Hello", csa);
                p0.complete();
            });
            delayed(1000, () -> {
                test.$testCallWithCB(System.currentTimeMillis(), (r, e) -> {
                    System.out.println(r+" "+Thread.currentThread().getName());
                    p1.complete();
                });
            });
            Thread t = Thread.currentThread();
            delayed(1000, () -> {
                test.$futureTest("ToBeDoubled").then( (r,e) -> {
                    if ( t != Thread.currentThread() )
                        error.incrementAndGet();
                    if ( count > 0 ) {
                        self().$runTest(test, csa, count-1).await();
                        p2.complete();
                    } else {
                        p2.complete();
                    }
                });
            });
            return all(p0,p1,p2);
        }
    }

    public static ServerTestFacade runWS(boolean b) throws Exception {
        ServerTestFacade test = WebSocketClient.Connect(ServerTestFacade.class, "ws://localhost:8080/ws", null).await();
        return runIt(b, test);
    }

    public static ServerTestFacade run( boolean bench ) throws Exception {
        ServerTestFacade test = TCPActorClient.Connect(ServerTestFacade.class, "localhost", 7777).await();
        return runIt(bench, test);
    }

    protected static ServerTestFacade runIt(boolean bench, ServerTestFacade test) {
        if (test != null) {
            if (bench) {
                long now = System.currentTimeMillis();
                while (System.currentTimeMillis()-now < 10000) {
//                        test.$benchMark(13, "this is a longish string");
                    test.$benchMark(13, null);
                }
                System.out.println("--------------- with future -------------------------");
                now = System.currentTimeMillis();
                while (System.currentTimeMillis()-now < 10000) {
//                        test.$benchMark(13, "this is a longish string");
                    test.$benchMark1(13, null);
                }
                test.$ping().await();
            } else {
                ClientSideActor csa = Actors.AsActor(ClientSideActor.class);
                TA t = Actors.AsActor(TA.class);
                t.$runTest(test, csa, 10).await();
            }
            return test;
        } else {
            throw new RuntimeException("Error");
        }
    }

}

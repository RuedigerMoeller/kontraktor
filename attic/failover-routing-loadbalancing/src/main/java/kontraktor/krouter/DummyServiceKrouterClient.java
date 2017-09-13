package kontraktor.krouter;

import kontraktor.krouter.service.DummyService;
import kontraktor.krouter.service.ForeignClass;
import org.nustaq.kontraktor.AwaitException;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.routers.Routing;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

public class DummyServiceKrouterClient {

    private static void runTest(DummyService routerClient) {

        runSimpleBench(routerClient);
//        runPromiseBench(routerClient);
//        System.out.println("main done");
        basics(routerClient,true, true);
    }

    public static void main(String[] args) {

        // connect to service via Krouter
        DummyService routerClient = (DummyService)
            Routing.connectClient(
                new WebSocketConnectable()
                    .url("ws://localhost:8888/binary")
                    .actorClass(DummyService.class)
                    .serType(SerializerType.FSTSer
                ),
                x -> System.exit(1)
            ).await();
        try {
            routerClient.ping().await(); // throws exception if not available
        } catch (AwaitException ae) {
            ae.printStackTrace();
            System.exit(1);
        }
        runTest(routerClient);
    }

    private static void runSimpleBench(DummyService routerClient) {
        System.out.println("run benchmark .. see service output");
        routerClient.resetSimpleBench();
        IntStream.range(0,50).forEach( ii -> {
            for (int i=0; i < 200_000; i++) {
                routerClient.simpleBench(i);
            }
            while ( routerClient.getMailboxSize() > 200_000 ) {
                Thread.yield();
            }
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runPromiseBench(DummyService routerClient) {
        System.out.println("run promise benchmark .. see service output");
        int[] out = {0};
        int[] in = {0};
        IntStream.range(0,100).forEach( ii -> {
            for (int i=0; i < 100_000; i++) {
                out[0]++;
                routerClient.roundTrip(out[0]).then(
                    (r,e) -> in[0]++
                );
            }
            while ( out[0] - in[0] > 100_000 )
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        });
        long now = System.currentTimeMillis();
        while( out[0] - in[0] > 0 && System.currentTimeMillis()-now < 5000)
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
    }

    private static void basics(DummyService routerClient, boolean subservice, boolean cyclic) {
//        if ( 1 != 1 )
        {
            routerClient.roundTrip(System.currentTimeMillis()).then((l, e) -> {
                if (l == null)
                    System.out.println(e);
                else
                    System.out.println("RT " + (System.currentTimeMillis() - (Long) l));
            });

            routerClient.pingForeign(new ForeignClass(2,4,6)).then( r -> {
                System.out.println("pingFor:"+r);
            });
            routerClient.subscribe(new ForeignClass(1, 2, 3), (r, e) -> {
                System.out.println("subs " + r + " " + e);
            });
        }
        if ( subservice ) {
            // subservices only works for sticky krouters (e.g. hotcold,hothot)
            // as with roundrobin the subservice will be created in one instance only
            routerClient.createSubService().then((subserv, err) -> {
                System.out.println("got subservice " + subserv);
                subserv.subMe("XX", (r, e) -> {
                    System.out.println("SUBCB " + r + " " + e);
                }).then((r, e) -> {
                    System.out.println("SUBPROM " + r + " " + e);
                });
                if ( cyclic ) {
                    String state = "Hello " + Math.random();
                    subserv.setState(state);
                    new Thread(()->{
                        while ( true ) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            subserv.getState().then( r -> {
                                if ( state.equals(r) ) {
                                    System.out.println("state ok");
                                } else {
                                    System.out.println("WRONG STATE "+r+" my:"+state);
                                }
                            });
                        }
                    }).start();
                }
            });
        }
    }
}

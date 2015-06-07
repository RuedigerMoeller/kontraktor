package needs_kontraktor_http_js_example_server;

import org.nustaq.kontraktor.barebone.Callback;
import org.nustaq.kontraktor.barebone.ConnectionListener;
import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.kontraktor.barebone.RemoteActorConnection;
import org.nustaq.serialization.coders.Unknown;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 06/06/15.
 *
 * expects the src/examples/http-ws-javascript server to run
 */
public class RunTestClient {

    public static void main(String[] args) throws InterruptedException {

        final Executor myWorkers = Executors.newCachedThreadPool();
        final RemoteActorConnection act = new RemoteActorConnection(
           new ConnectionListener() {
               @Override
               public void connectionClosed(String s) {
                   System.out.println("connection closed");
               }
           },
           false
        );

        final RemoteActor facade = act.connect("http://localhost:8080/api", true).await();
        System.out.println("facade:" + facade);

        RemoteActor session = (RemoteActor) facade.ask("login", "user", "password").await();
        System.out.println("session Actor received: " + session);

        session.ask("getToDo").then(new Callback<ArrayList>() {
            @Override
            public void receive(ArrayList result, Object error) {
                for (int i = 0; i < result.size(); i++) {
                    Object o = result.get(i);
                    System.out.println(o);
                }
            }
        });

        session.ask("getPojo").then(new Callback() {
            @Override
            public void receive(Object result, Object error) {
                System.out.println("pojo received ! "+result);
            }
        });

        Unknown uk = new Unknown("sample.httpjs.Pojo")
            .set("name", "Hello")
            .set("otherPojos",
                new Unknown("set")
                    .add(2)
                    .add( new Unknown("sample.httpjs.Pojo").set("name", "XX") )
                    .add( new Unknown("sample.httpjs.Pojo").set("name", "YY") )
            );
        session.ask("pojoRoundTrip", uk).then(new Callback() {
            @Override
            public void receive(Object result, Object error) {
                System.out.println("roundtrip:"+result);
            }
        });

        session.tell("subscribe", new Callback() {
            @Override
            public void receive(Object result, Object error) {
                System.out.println("event received:" + result);
            }
        });

        myWorkers.execute(new Runnable() {
            @Override
            public void run() {
                while (System.currentTimeMillis() == 0 || true) {
                    long tim = System.currentTimeMillis();
                    final AtomicInteger count = new AtomicInteger();
                    final boolean res[] = new boolean[100_000];
                    for (int i = 0; i < 100_000; i++) {
                        count.incrementAndGet();
                        // backpressure disallow more than 1000 outstanding requests
                        while (act.getOpenFutureRequests() > 1000) {
                            LockSupport.parkNanos(1000L * 1000L); // block 1 ms
                        }
                        facade.ask("promiseMethodForBenchmark", i).then(new Callback<Integer>() {
                            @Override
                            public void receive(Integer result, Object error) {
                                count.decrementAndGet();
                                res[result] = true;
                            }
                        });
                    }
                    while (count.get() != 0) { // wait for all replies
                        LockSupport.parkNanos(1000L * 1000L);
                    }
                    for (int i = 0; i < res.length; i++) {
                        if (!res[i]) {
                            System.out.println("Error " + i);
                        }
                    }
                    System.out.println("100k future calls took " + (System.currentTimeMillis() - tim) + " ms");
                }
            }
        });
        Thread.sleep(100000);
    }

}

package needs_kontraktor_http_js_example_server;

import org.nustaq.kontraktor.barebone.Callback;
import org.nustaq.kontraktor.barebone.ConnectionListener;
import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.kontraktor.barebone.RemoteActorConnection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 11/06/15.
 */
public class LoadTest {

    static AtomicInteger connections = new AtomicInteger();

    int count = 0;
    AtomicLong lastBcast = new AtomicLong(System.currentTimeMillis());
    volatile RemoteActor session;
    volatile RemoteActorConnection act;

    public void run() {
        act = new RemoteActorConnection(
           new ConnectionListener() {
               @Override
               public void connectionClosed(String s) {
                   System.out.println("connection closed");
               }
           },
           false
        );

        final RemoteActor facade = act.connect("http://localhost:8080/api", true).await(20_000);

        System.out.println("CLIENTS:"+connections.incrementAndGet());

        session = (RemoteActor) facade.ask("login", "user", "password").await(20_000);
        session.tell("subscribe", new Callback() {
            @Override
            public void receive(Object result, Object error) {
                count++;
                lastBcast.set(System.currentTimeMillis());
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        final ArrayList<LoadTest> tests = new ArrayList<>();
        for ( int n = 0; n < 50; n++ ) {
            new Thread() {
                public void run() {
                    LoadTest loadTest = new LoadTest();
                    synchronized (tests) {
                        tests.add(loadTest);
                    }
                    loadTest.run();
                }
            }.start();
        }
        Thread.sleep(6000);
        while( true ) {
            synchronized (tests) {
                for (int i = 0; i < tests.size(); i++) {
                    LoadTest loadTest = tests.get(i);
                    long l = System.currentTimeMillis() - loadTest.lastBcast.get();
                    if ( l > 3000 ) {
                        System.out.println("client "+loadTest.session+" is late:"+l);
                        if ( l > 20_000 ) {
                            loadTest.act.close();
                            tests.remove(i);
                            i--;
                            System.out.println("clients remaining "+tests.size());
                        }
                    }
                }
            }
            Thread.sleep(2000);
        }
    }
}

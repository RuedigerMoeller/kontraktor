package sample.test;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import sample.httpjs.MyHttpApp;
import sample.httpjs.MyHttpAppSession;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 13/06/15.
 *
 * requires example/http-ws-javascript to run
 */
public class KLoadTest {

    int count = 0;
    AtomicLong lastBcast = new AtomicLong(System.currentTimeMillis());
    MyHttpAppSession session;
    MyHttpApp myApp;

    public void run() {
        boolean http = false;
        ConnectableActor connectable;
        if ( !http ) {
            connectable = new WebSocketConnectable(MyHttpApp.class, "ws://localhost:8080/ws")
                                 .serType(SerializerType.JsonNoRef);
        } else {
            connectable = new HttpConnectable(MyHttpApp.class, "http://localhost:8080/api")
                                 .serType(SerializerType.JsonNoRef);
        }

        connectable
            .connect((connector, error) -> {
                System.out.println("connection lost " + connector);
            }).then( (res,err) -> {
                myApp = (MyHttpApp) res;
                myApp.login("dummy", "dummy").then( (s,err1) -> {
                    session = s;
                    System.out.println("session connected "+session);
                    session.subscribe((result, e) -> {
                        count++;
                        lastBcast.set(System.currentTimeMillis());
                    });
                });
            });
    }

    public static void main(String[] args) throws InterruptedException {
        HttpClientConnector.MAX_CONN_PER_ROUTE = 2000;
        HttpClientConnector.MAX_CONN_TOTAL = 2000;
        final ArrayList<KLoadTest> tests = new ArrayList<>();
        for ( int i = 0; i <10; i++ ) {
            for ( int n = 0; n < 80; n++ ) {
                KLoadTest loadTest = new KLoadTest();
                synchronized (tests) {
                    tests.add(loadTest);
                }
                loadTest.run();
            }
            Thread.sleep(5_000);
        }
        while( true ) {
            synchronized (tests) {
                for (int i = 0; i < tests.size(); i++) {
                    KLoadTest loadTest = tests.get(i);
                    long l = System.currentTimeMillis() - loadTest.lastBcast.get();
                    if ( l > 3000 ) {
                        System.out.println("client "+loadTest.session+" is late:"+l);
                        if ( l > 20_000 ) {
                            if ( loadTest.myApp != null )
                                loadTest.myApp.close();
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

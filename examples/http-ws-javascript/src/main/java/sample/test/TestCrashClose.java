package sample.test;

import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import sample.httpjs.MyHttpApp;
import sample.httpjs.MyHttpAppSession;

/**
 * Created by ruedi on 05/07/15.
 */
public class TestCrashClose {

    public static void main(String[] args) {

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
                MyHttpApp myApp = (MyHttpApp) res;
                myApp.login("dummy", "dummy").then( (s,err1) -> {
                    MyHttpAppSession session = s;
                    System.out.println("session connected "+session);
                    session.subscribe((result, e) -> {
                        System.exit(0);
                    });
                });
            });

    }
}

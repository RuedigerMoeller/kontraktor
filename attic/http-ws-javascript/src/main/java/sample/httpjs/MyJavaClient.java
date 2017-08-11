package sample.httpjs;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

/**
 * Created by ruedi on 30/05/15.
 *
 * Dev only. A pure java client useful to figure out JSON formatting of request/responses
 *
 */
public class MyJavaClient {

    public static void main(String[] args) {
        boolean http = false;
        MyHttpApp remoteApp;
        if ( http ) {
            HttpClientConnector.DumpProtocol = true;
            remoteApp = (MyHttpApp)
                new HttpConnectable(MyHttpApp.class, "http://localhost:8080/api")
                    .serType(SerializerType.JsonNoRefPretty)
                    .connect()
                    .await();
        } else {
            JSR356ClientConnector.DumpProtocol = true; // dev only
            remoteApp = (MyHttpApp)
                new WebSocketConnectable(MyHttpApp.class, "ws://localhost:8080/ws")
                    .serType(SerializerType.JsonNoRefPretty)
                    .connect()
                    .await();
        }

        MyHttpAppSession session = remoteApp.login("someuser", "apwd").await();
        session.getToDo().then( list ->  {
            list.forEach(System.out::println);
        });
        session.streamToDo( "p", (r,e) -> System.out.println(r+" "+e) );
    }
}

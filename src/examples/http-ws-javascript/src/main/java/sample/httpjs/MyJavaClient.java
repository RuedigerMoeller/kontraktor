package sample.httpjs;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;

/**
 * Created by ruedi on 30/05/15.
 *
 * Dev only. A pure java client useful to figure out JSON formatting of request/responses
 *
 */
public class MyJavaClient {

    public static void main(String[] args) {
        boolean http = true;
        MyHttpApp remoteApp;
        if ( http ) {
            HttpClientConnector.DumpProtocol = true;
            remoteApp = HttpClientConnector.Connect(MyHttpApp.class, "http://localhost:8080/api", null, null, new Coding(SerializerType.JsonNoRefPretty)).await();
        } else {
            JSR356ClientConnector.DumpProtocol = true; // dev only
            remoteApp = JSR356ClientConnector.Connect(MyHttpApp.class, "ws://localhost:8080/ws", new Coding(SerializerType.JsonNoRefPretty)).await();
        }

        MyHttpAppSession session = remoteApp.login("someuser", "apwd").await();
        session.getToDo().then( list ->  {
            list.forEach(System.out::println);
        });
        session.streamToDo( "p", (r,e) -> System.out.println(r+" "+e) );
    }
}

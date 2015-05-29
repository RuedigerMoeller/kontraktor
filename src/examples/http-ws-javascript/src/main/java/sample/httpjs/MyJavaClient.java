package sample.httpjs;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;

/**
 * Created by ruedi on 30/05/15.
 */
public class MyJavaClient {

    public static void main(String[] args) {
        JSR356ClientConnector.DumpProtocol = true; // dev only
        MyHttpApp remoteApp = JSR356ClientConnector.Connect(MyHttpApp.class, "ws://localhost:8080/ws", new Coding(SerializerType.JsonNoRefPretty)).await();
        MyHttpAppSession session = remoteApp.login("user", "pwd").await();
        session.getToDo().then( list ->  {
            list.forEach(System.out::println);
        });
    }
}

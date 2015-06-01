package sample.httpjs;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.nustaq.kontraktor.*;

import static org.nustaq.kontraktor.Actors.*;

import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.fourk.Http4K;
import org.nustaq.kontraktor.util.Pair;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by ruedi on 29/05/15.
 */
public class MyHttpApp extends Actor<MyHttpApp> {

    Scheduler clientThread = new SimpleScheduler(1000);

    public IPromise<String> getServerTime() {
        return new Promise<>(new Date().toString());
    }

    public IPromise<MyHttpAppSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session. All sessions share one thread (scheduler). Create several schedulers to scale up
            MyHttpAppSession sess = AsActor(MyHttpAppSession.class,clientThread);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), Arrays.asList("procrastinize", "ignore *") );
            result.resolve(sess);
        }
        return result;
    }

    public void clientClosed(MyHttpAppSession session) {
        System.out.println("client closed "+session);
    }

    public static void main(String[] args) {
        String hostName = "localhost"; int port = 8080;
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-ws-client");
            System.exit(-1);
        }

        Http4K.get().publishFileSystem(hostName, "/", port, root);

        MyHttpApp myHttpApp = AsActor(MyHttpApp.class);
        Http4K.get().publishOnWebSocket( myHttpApp, hostName,"/ws", port, new Coding(SerializerType.JsonNoRefPretty) );
    }

}

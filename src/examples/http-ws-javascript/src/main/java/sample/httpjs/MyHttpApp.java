package sample.httpjs;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.fourk.Http4K;
import org.nustaq.kontraktor.remoting.http.UndertowHttpServerConnector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 29/05/15.
 */
public class MyHttpApp extends Actor<MyHttpApp> {

    Scheduler clientThreads[] = {
        new SimpleScheduler(1000) // only one session processor threads. All session actor use this.
    };

    public IPromise<String> getServerTime() {
        return new Promise<>(new Date().toString());
    }

    public IPromise<MyHttpAppSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session and assign it a random scheduler (~thread). Note that with async nonblocking style
            // one thread will be sufficient. For real computing intensive apps increase clientThreads to like 2-4
            MyHttpAppSession sess = AsActor(MyHttpAppSession.class,clientThreads[((int) (Math.random() * clientThreads.length))]);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), Arrays.asList("procrastinize", "drink coffee", "code", "play the piano", "ignore *") );
            result.resolve(sess);
        }
        return result;
    }

    public IPromise<Integer> getNumSessions() {
        return resolve(clientThreads[0].getNumActors());
    }

    public void clientClosed(MyHttpAppSession session) {
        System.out.println("client closed "+session);
    }

    public static void main(String[] args) throws IOException {
        // currently no configuration framework classes for this is provided,
        // just setup stuff manually here. Its easy to build a application specific
        // config using e.g. json or kson.
        String hostName = "localhost"; int port = 8080;
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-ws-javascript");
            System.exit(-1);
        }

        // override and set Http4k singleton in order to modify default config of
        // undertow (used as webserver). However default configs will be ok for many apps

        // link index.html and jsk.js dir to avoid copying
        Http4K.get().publishFileSystem(hostName, "/", port, root);
        Http4K.get().publishFileSystem(hostName, "/jsk", port, new File(root.getCanonicalPath()+"/../../../main/javascript/"));

        // create and publish server actor
        MyHttpApp myHttpApp = AsActor(MyHttpApp.class);

        Http4K.get().publishOnWebSocket( myHttpApp, hostName,"/ws", port, new Coding(SerializerType.JsonNoRefPretty) );
        Http4K.get().publishOnHttp( myHttpApp, hostName, "/api", port, new Coding(SerializerType.JsonNoRefPretty) )
            .then( server -> ((UndertowHttpServerConnector)server.getConnector()).setSessionTimeout(30000) );
    }

}

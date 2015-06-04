package sample.httpjs;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * Created by ruedi on 04/06/15.
 *
 * Just serup and start server
 *
 */
public class MyHttpAppMain {

    public static void main(String[] args) throws IOException {
        // just setup stuff manually here. Its easy to build a application specific
        // config using e.g. json or kson.
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-ws-javascript");
            System.exit(-1);
        }

        // create server actor
        MyHttpApp myHttpApp = AsActor(MyHttpApp.class);

        HttpPublisher pub = new HttpPublisher( myHttpApp, "localhost", "/api", 8080 )
            .serType(SerializerType.JsonNoRefPretty)
            .setSessionTimeout(30000);

        // link index.html and js4k.js dir to avoid copying stuff around my project
        Http4K.get()
            .publishFileSystem(pub, "/", root)
            .publishFileSystem(pub, "/jsk", new File(root.getCanonicalPath() + "/../../../main/javascript/"));

        // publish as long poll @ localhost:8080/api
        pub.publish();
        // and as websocket @ localhost:8080/ws
        pub.toWS().urlPath("/ws").publish();

    }

}

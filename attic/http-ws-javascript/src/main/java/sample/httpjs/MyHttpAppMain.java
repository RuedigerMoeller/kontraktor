package sample.httpjs;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

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
        // just setup stuff manually here. Its easy to buildResourcePath an application specific
        // config using e.g. json or kson.
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-ws-javascript");
            System.exit(-1);
        }

        // link index.html and js4k.js dir to avoid copying stuff around the project
        File jsroot = new File(root.getCanonicalPath() + "/../../../modules/kontraktor-http/src/main/javascript/js4k").getCanonicalFile();

        // create server actor
        MyHttpApp myHttpApp = AsActor(MyHttpApp.class);
        myHttpApp.init();

        Class msgClasses[] = { Address.class };
        Http4K.Build("localhost", 8080)
            .fileRoot("/", root)
            .fileRoot("/jsk", jsroot)
            .httpAPI("/api", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(30_000)
                .buildHttpApi()
            .websocket("/ws", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                // replace serType like below to provide classes which are encoded using simple names (no fqclassnames)
//                .coding(new Coding(SerializerType.JsonNoRef, msgClasses ))
                .build()
            .build();
    }

}

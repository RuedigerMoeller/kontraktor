package sample.polymer;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.Http4K;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * Created by ruedi on 13/07/15.
 */
public class PolymerAppMain {

    public static void main(String[] args) throws IOException {
        // just setup stuff manually here. Its easy to build an application specific
        // config using e.g. json or kson.
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/spa-polymer");
            System.exit(-1);
        }

        // create server actor
        PolymerApp app = AsActor(PolymerApp.class);

        Http4K.Build("localhost", 8080)
            .fileRoot( "/", root)
            .httpAPI(  "/api", app)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(30_000)
                .build()
            .websocket("ws", app )
                .serType(SerializerType.JsonNoRef)
                .build()
            .resourcePath( "/dyn" )
                .rootComponent("app")
                .resourcePath(
                    "./web",
                    "./web/lib",
                    "../../modules/kontraktor-http/src/main/javascript",
                    "./web/lib")
                .devMode(true)
                .build()
            .build();
    }

}

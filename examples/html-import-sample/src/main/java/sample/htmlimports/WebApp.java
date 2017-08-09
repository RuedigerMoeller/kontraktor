package sample.htmlimports;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

import java.io.File;

public class WebApp extends Actor<WebApp> {

    @Local
    public void init() {

    }

    public IPromise greet(String name) {
        return resolve("Hello "+name);
    }

    public static void main(String[] args) {
        boolean DEVMODE = true;

        // just setup stuff manually here. Its easy to buildResourcePath an application specific
        // config using e.g. json or kson.
        File root = new File("./src/main/web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-import-sample");
            System.exit(-1);
        }

        // create server actor
        WebApp myHttpApp = AsActor(WebApp.class);
        myHttpApp.init(); // async !

        Http4K.Build("localhost", 8081)
            .resourcePath("/")
                .elements("./src/main/web","src/main/node_modules")
                .allDev(DEVMODE)
                .buildResourcePath()
            .httpAPI("/api", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(5*60_000)
                .buildHttpApi()
            .build();

    }

}

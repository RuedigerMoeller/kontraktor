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
        boolean DEVMODE = true;

        // just setup stuff manually here. Its easy to buildResourcePath an application specific
        // config using e.g. json or kson.
        File root = new File("./web");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/http-ws-javascript-es6");
            System.exit(-1);
        }

        // create server actor
        MyHttpApp myHttpApp = AsActor(MyHttpApp.class);
        myHttpApp.init();

        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements("./web","./web/node_modules")
                .allDev(DEVMODE)
                .handlerInterceptor( exchange -> {
                    // can be used to intercept (e.g. redirect or raw response) all requests coming in on this resourcepath
                    // e.g. confirmation links from a registration, generated resources
                    String requestPath = exchange.getRequestPath();
                    if ( requestPath == null || !requestPath.startsWith("/inlink/") ) {
                        return false;
                    }
                    exchange.dispatch();
                    myHttpApp.handleDirectRequest(exchange);
                    return true;
                })
                .buildResourcePath()
            .httpAPI("/api", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(5*60_000)
                .buildHttpApi()
            .websocket("/ws", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .build()
            .build();
    }

}

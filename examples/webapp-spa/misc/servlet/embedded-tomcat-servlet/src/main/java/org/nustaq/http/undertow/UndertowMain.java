package org.nustaq.http.undertow;

import org.nustaq.http.example.ServletApp;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * Created by ruedi on 19.06.17.
 *
 * debugging purposes
 *
 */
public class UndertowMain {

    private static final boolean DEV = true;

    public static void main(String[] args) throws IOException {
        // just setup stuff manually here. Its easy to buildResourcePath an application specific
        // config using e.g. json or kson.
        File root = new File("./src/main/webapp/client");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]examples/servlet");
            System.exit(-1);
        }

        // create server actor
        ServletApp myHttpApp = AsActor(ServletApp.class);
        myHttpApp.init(4);

        Class msgClasses[] = {};
        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements(root.getPath(),root.getPath()+"/../lib")
                .allDev(DEV)
                .buildResourcePath()
            .httpAPI("/ep", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(30_000)
                .buildHttpApi()
            .websocket("/ws", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                // replace serType like below to provide classes which are encoded using simple names (no fqclassnames)
//                .coding(new Coding(SerializerType.JsonNoRef, msgClasses ))
                .buildWebsocket()
            .build();
    }

}


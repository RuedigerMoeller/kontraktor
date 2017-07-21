package org.nustaq.reactsample;

/**
 * Created by ruedi on 21.07.17.
 */

import org.nustaq.kontraktor.babel.BabelOpts;
import org.nustaq.kontraktor.babel.JSXTranspiler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.weblication.BasicAuthenticationResult;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;
import org.nustaq.kontraktor.weblication.PersistedRecord;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * configure + start server. Requires working dir in project root ([..]examples/react)
 */
public class ReactAppMain {

    public static void main(String[] args) throws IOException {
        boolean DEV = true;

        // start node babelserver daemon directly, uncomment if you prefer to run it manually (avoids restarting it with each server start)
        if ( !BasicWebAppActor.runNodify() ) {
            System.out.println("failed to connect / start babel");
            System.exit(1);
        }

        // just setup stuff manually here. Its easy to build an application specific
        // config using e.g. json or kson.
        File root = new File("./src/main/web/client");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]examples/react");
            System.exit(-1);
        }

        // create server actor
        ReactApp myHttpApp = AsActor(ReactApp.class);
        myHttpApp.init(new BasicWebAppConfig());

        Class msgClasses[] = { BasicAuthenticationResult.class, PersistedRecord.class }; // these classes are encoded with Simple Name in JSon
        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements(
                    "src/main/web/client",
                    "src/main/web/client/node_modules"
                )
                .allDev(DEV)
                .transpile("jsx",new JSXTranspiler().opts(new BabelOpts().debug(DEV)))
                .build()
            .httpAPI("/ep", myHttpApp)
                .coding(new Coding(SerializerType.JsonNoRef,msgClasses))
                .setSessionTimeout(30_000)
                .build()
            .build();
    }

}

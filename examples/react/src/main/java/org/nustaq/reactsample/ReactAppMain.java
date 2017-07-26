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

        // check/start node babelserver daemon (if this fails run babeldaemon manually, e.g. windoze
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

        // these classes are encoded with Simple Name in JSon
        Class msgClasses[] = { BasicAuthenticationResult.class, PersistedRecord.class };
        Http4K.Build("localhost", 8080)
            .fileRoot("static", "src/main/web/static")
            .resourcePath("/")
                .elements(
                    "src/main/web/client",
                    "src/main/web/client/node_modules"
                )
                .allDev(DEV)
                .transpile("jsx",new JSXTranspiler().opts(new BabelOpts().debug(DEV)))
                .handlerInterceptor( exchange -> {
                    // can be used to intercept (e.g. redirect or raw response) all requests coming in on this resourcepath
                    String requestPath = exchange.getRequestPath();
                    if ( requestPath == null || !requestPath.startsWith("/direct/") ) {
                        return false;
                    }
                    exchange.dispatch();
                    myHttpApp.handleDirectRequest(exchange);
                    return true;
                })
                .build()
            .httpAPI("/ep", myHttpApp)
                .coding(new Coding(SerializerType.JsonNoRef,msgClasses))
                .setSessionTimeout(30_000)
                .build()
            .build();
    }

}

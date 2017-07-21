package org.nustaq.reactsample;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.annotations.Secured;
import org.nustaq.kontraktor.babel.BabelOpts;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.babel.JSXTranspiler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicAuthenticationResult;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;
import org.nustaq.kontraktor.weblication.PersistedRecord;

import java.io.*;

/**
 * Created by ruedi on 30.05.17.
 */
@Secured // enforce explicit tagging of remoteable methods (@Remote) effects ALSO Session actors
public class ReactApp extends BasicWebAppActor<ReactApp,BasicWebAppConfig> {

    @Override
    protected Class getSessionClazz() {
        return ReactAppSession.class;
    }

    @Remoted
    public IPromise register(String nick, String pwd, String text) {
        Promise p = new Promise();
        sessionStorage.storeIfNotPresent(new PersistedRecord(nick).put("pwd",pwd).put("text",text)).then( (r,e) -> {
            if ( ! r ) {
                p.reject("user "+nick+" already exists");
            } else {
                p.resolve(true);
            }
        });
        return p;
    }

    /**
     * return new Promise(null) in order to bypass session reanimation default implementation
     */
//    @Override
//    public IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
//        Log.Info(this,"reanimate called ..");
//        return new Promise<>(null);
//    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws IOException {
        boolean DEV = true;

        // start node babelserver daemon directly, uncomment if you prefer to run it manually (avoids restarting it with each server start)
        if ( ! runNodify() ) {
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

        Class msgClasses[] = {BasicAuthenticationResult.class};
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
//            .websocket("/ws", myHttpApp)
//                .coding(new Coding(SerializerType.JsonNoRef,msgClasses))
//                .build()
            .build();
    }


}

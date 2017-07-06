package org.nustaq.reactsample;

import org.nustaq.babelremote.BabelOpts;
import org.nustaq.babelremote.BrowseriBabelify;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

import java.io.*;

/**
 * Created by ruedi on 30.05.17.
 */
public class ReactApp extends BasicWebAppActor<ReactApp,BasicWebAppConfig> {

    @Override
    protected IPromise<String> verifyCredentials(String s, String pw, String jwt) {
        return new Promise<>("logged in");
    }

    @Override
    protected Class getSessionClazz() {
        return ReactAppSession.class;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean runNodify() {
        try {
            BrowseriBabelify.get();
        } catch (Exception ex) {
            Log.Warn(ReactApp.class,"babelserver not running .. try starting");
            boolean isWindows = System.getProperty("os.name","linux").toLowerCase().indexOf("windows") >= 0;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (isWindows) {
                    processBuilder.command("cmd.exe", "/c", "node babelserver.js");
                } else {
                    String bash = "/usr/bin/bash";
                    if ( !new File(bash).exists() ) {
                        bash = "/bin/bash";
                    }
                    processBuilder.command(bash, "-c", "node babelserver.js");
                }
                processBuilder.directory(new File("./src/main/nodejs"));
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                Thread.sleep(1000);
                BrowseriBabelify.get();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        return true;
    }

    public static void main(String[] args) throws IOException {

        // start node babelify daemon directly, uncomment if you prefer to run it manually (avoids restarting it with each server start)
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

        Class msgClasses[] = {};
        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements(
                    "src/main/web/client",
                    "src/main/web/lib",
                    "src/main/web/bower_components",
                    "src/main/nodejs"
                )
                .allDev(true)
                .transpile("jsx",new JSXTranspiler().opts(new BabelOpts().debug(true))) // "'react','es2015'" for es5 output
                .build()
            .httpAPI("/ep", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(30_000)
                .build()
            .websocket("/ws", myHttpApp)
                .serType(SerializerType.JsonNoRef)
                // replace serType like below to provide classes which are encoded using simple names (no fqclassnames)
    //                .coding(new Coding(SerializerType.JsonNoRef, msgClasses ))
                .build()
            .build();
    }


}

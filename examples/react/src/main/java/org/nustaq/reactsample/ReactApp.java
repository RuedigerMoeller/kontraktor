package world.united.mixereum;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

import java.io.File;
import java.io.IOException;

/**
 * Created by ruedi on 30.05.17.
 */
public class ReactApp extends BasicWebAppActor<ReactApp,BasicWebAppConfig> {

    @Override
    protected IPromise<String> verifyCredentials(String s, String pw, String jwt) {
        return null;
    }

    @Override
    protected Class getSessionClazz() {
        return null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws IOException {
        // just setup stuff manually here. Its easy to build an application specific
        // config using e.g. json or kson.
        File root = new File("./src/main/webapp");

        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]examples/react-sample");
            System.exit(-1);
        }

        // link index.html and js4k.js dir to avoid copying stuff around the project
        File jsroot = new File(root.getCanonicalPath() + "/../../../modules/kontraktor-http/src/main/javascript/js4k").getCanonicalFile();

        // create server actor
        ReactApp myHttpApp = AsActor(ReactApp.class);
        myHttpApp.init(new BasicWebAppConfig());

        Class msgClasses[] = {};
        Http4K.Build("localhost", 8080)
                .fileRoot("/", root)
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

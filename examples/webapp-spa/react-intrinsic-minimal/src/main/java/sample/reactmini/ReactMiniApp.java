package sample.reactmini;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * minimal implementation of session based server (incl. load balancing)
 */
public class ReactMiniApp extends Actor<ReactMiniApp> {

    private Scheduler clientThreads[];
    private Random rand = new Random();

    @Local
    public void init(int nthreads) {
        clientThreads = new Scheduler[nthreads];
        IntStream.range(0,nthreads)
            .forEach( i -> clientThreads[i] = new SimpleScheduler(100, true /*Important!*/ ));
    }

    public IPromise<ReactMiniSession> login(String username) {
        if ( "".equals(username.trim()) ) {
            return reject("empty username");
        }
        ReactMiniSession session = AsActor(
            ReactMiniSession.class,
            // randomly distribute session actors among clientThreads
            clientThreads[rand.nextInt(clientThreads.length)]
        );
        session.init(username);
        return resolve(session); // == new Promise(session)
    }

    public static void main(String[] args) {
        boolean DEVMODE = true;

        if ( ! new File("./src/main/web/client/index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/react-intrinsic-minimal");
            System.exit(-1);
        }

        ReactMiniApp app = AsActor(ReactMiniApp.class);
        app.init(4);

        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements("./src/main/web/client","./src/main/web/lib")
                .transpile("jsx", new JSXIntrinsicTranspiler(DEVMODE,!DEVMODE))
                .allDev(DEVMODE)
                .buildResourcePath()
            .httpAPI("/api", app)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(TimeUnit.MINUTES.toMillis(30))
                .buildHttpApi()
            .build();
    }
}

package sample.reactmaterial;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * minimal implementation of session based server (incl. load balancing)
 */
public class ReactMaterialUITestApp extends Actor<ReactMaterialUITestApp> implements SessionResurrector {

    private Scheduler clientThreads[];
    private Random rand = new Random();
    private PersistanceDummy persistance = new PersistanceDummy();

    @Local
    public void init(int nthreads) {
        clientThreads = new Scheduler[nthreads];
        IntStream.range(0,nthreads)
            .forEach( i -> clientThreads[i] = new SimpleScheduler(100, true /*Important!*/ ));
        cycle();
    }

    public IPromise<ReactMaterialUITestSession> login(String username) {
        if ( "".equals(username.trim()) ) {
            return reject("empty username");
        }
        ReactMaterialUITestSession session = AsActor(
            ReactMaterialUITestSession.class,
            // randomly distribute session actors among clientThreads
            clientThreads[rand.nextInt(clientThreads.length)]
        );
        session.init(username,self());
        return resolve(session); // == new Promise(session)
    }

    @Override
    public IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
        // dummy in memory
        String userName = (String) persistance.getSessionData(sessionId);
        if ( userName != null ) {
            // create a new session with stored data, client is notified
            // in case it needs to refresh client side data
            Log.Info(this,"reanimated session "+sessionId+" with data "+userName);
            return (IPromise)login(userName);
        }
        return resolve(null); // cannot reanimate => client shows "session expired"
    }

    void cycle() {
        if ( ! isStopped() ) {
            // sessions are remembered fo 5 days
            delayed(TimeUnit.DAYS.toMillis(5), () -> {
                persistance.flipSessionCache();
                cycle();
            });
        }
    }

    @Local
    public void registerSessionData(String id, Object data) {
        Log.Info(this,"session "+id+" is "+data);
        persistance.putSessionData(id,data);
    }

    public static void main(String[] args) {
        boolean DEVMODE = true;

        if ( ! new File("./src/main/web/client/index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]/react-material-ui");
            System.exit(-1);
        }

        ReactMaterialUITestApp app = AsActor(ReactMaterialUITestApp.class);
        app.init(4);

        Http4K.Build("localhost", 8080)
            .resourcePath("/")
                .elements("./src/main/web/client","./src/main/web/lib","./src/main/web/node_modules")
                .transpile("jsx",
                    new JSXIntrinsicTranspiler(DEVMODE)
                        .configureJNPM("./src/main/web/node_modules","./src/main/web/jnpm.kson")
                        .autoJNPM(DEVMODE)
                )
                .allDev(DEVMODE)
                .buildResourcePath()
            .httpAPI("/api", app)
                .serType(SerializerType.JsonNoRef)
                .setSessionTimeout(10_000) // extra low to showcase session resurrection
                .buildHttpApi()
            .websocket("/ws",app)
                .serType(SerializerType.JsonNoRef)
                .sendSid(true)
                .buildWebsocket()
            .build();
    }

}

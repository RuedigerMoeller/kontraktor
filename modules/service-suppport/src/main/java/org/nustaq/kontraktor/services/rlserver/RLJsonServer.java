package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.apputil.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.MapRecord;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RLJsonServer<T extends RLJsonServer> extends Actor<T> {

    public final static String T_CREDENTIALS = "credentials";

    protected static SimpleRLConfig cfg = SimpleRLConfig.read();

    // threads to dispatch session onto
    protected Scheduler clientThreads[];
    protected Random rand = new Random();
    protected RLJsonServerService service;
    protected DataClient dclient;

    @Local
    public IPromise init(String args[]) {
        clientThreads = new Scheduler[cfg.getNumSessionThreads()];
        IntStream.range(0,cfg.getNumSessionThreads())
            .forEach( i -> clientThreads[i] = new SimpleScheduler(10000, true /*Important!*/ ));
        service = RLJsonServerService.start(args);
        service.setWebServer(self());
        dclient = service.getDClient();
        return resolve();
    }

    public IPromise<RLJsonAuthResult> authenticate(String user, String pwd ) {
        Log.Info(this,"authenticate session");
        return createSession(new Object[]{ user,pwd });
    }

    protected IPromise<RLJsonAuthResult> createSession(Object customSessionData) {
        // FIXME, check credentials
        RLJsonSession session = AsActor(
            getSessionActorClazz(customSessionData),
            // randomly distribute session actors among clientThreads
            clientThreads[rand.nextInt(clientThreads.length)]
        );
        session.init(self(),dclient,customSessionData);
        return resolve(new RLJsonAuthResult().session(session));
    }

    protected Class<? extends RLJsonSession> getSessionActorClazz(Object authData) {
        return RLJsonSession.class;
    }

    @CallerSideMethod
    protected void createServer(RLJsonServer app, Class[] CLAZZES) {
        Http4K.Build(cfg.getBindIp(), cfg.getBindPort())
            .httpAPI("/api", app) // could also be websocket based (see IntrinsicReactJSX github project)
            .coding(new Coding(SerializerType.JsonNoRef, CLAZZES))
            .setSessionTimeout(TimeUnit.MINUTES.toMillis(cfg.getSessionTimeoutMinutes() ))
            .buildHttpApi()
            .websocket("/ws", app)
                .coding(new Coding(SerializerType.JsonNoRef, CLAZZES))
                .buildWebsocket()
            .build();
    }

    public static void main(String[] args) throws InterruptedException {
        Class<RLJsonServer> appClazz = RLJsonServer.class;

        startUp(args, appClazz);

    }

    public static void startUp(String[] args, Class appClazz) throws InterruptedException {
        if ( ! new File("./etc").exists() ) {
            System.out.println("Please run with project working dir");
            System.exit(-1);
        }

        if ( cfg.runDataClusterInsideWebserver ) {
            SingleProcessRLCluster.main(new String[0]);
        }

        RLJsonServer app = (RLJsonServer) AsActor(appClazz);
        app.init(args).await(60_000);

        Class CLAZZES[] = {
            LoginData.class,
            MapRecord.class,
            AddMessage.class,
            UpdateMessage.class,
            RemoveMessage.class,
            QueryDoneMessage.class,
            SessionEvent.class,
            RLJsonAuthResult.class,
            Diff.class,
        };

        Log.Info(appClazz,"listening on http://"+cfg.getBindIp()+":"+cfg.getBindPort());

        app.createServer(app, CLAZZES);
    }


}

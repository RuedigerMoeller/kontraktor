package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.apputil.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.webapp.javascript.clojure.ClojureJSPostProcessor;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.MapRecord;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RLJsonServer extends Actor<RLJsonServer> {

    static SimpleRLConfig cfg = SimpleRLConfig.read();

    // threads to dispatch session onto
    private Scheduler clientThreads[];
    private Random rand = new Random();
    private RLJsonServerService service;
    private DataClient dclient;

    @Local
    public void init(String args[]) {
        clientThreads = new Scheduler[cfg.getNumSessionThreads()];
        IntStream.range(0,cfg.getNumSessionThreads())
            .forEach( i -> clientThreads[i] = new SimpleScheduler(10000, true /*Important!*/ ));
        service = RLJsonServerService.start(args);
        service.setWebServer(self());
        dclient = service.getDClient();
    }

    public static void main(String[] args) {
        if ( ! new File("./etc").exists() ) {
            System.out.println("Please run with project working dir");
            System.exit(-1);
        }

        RLJsonServer app = AsActor(RLJsonServer.class);
        app.init(args);

        Class CLAZZES[] = {
            LoginData.class,
            MapRecord.class,
            AddMessage.class,
            UpdateMessage.class,
            RemoveMessage.class,
            QueryDoneMessage.class,
            SessionEvent.class,
            Diff.class,
        };

        Http4K.Build(cfg.getBindIp(), cfg.getBindPort())
            .httpAPI("/api", app) // could also be websocket based (see IntrinsicReactJSX github project)
                .coding(new Coding(SerializerType.JsonNoRef, CLAZZES))
                .setSessionTimeout(TimeUnit.MINUTES.toMillis(cfg.getSessionTimeoutMinutes() ))
                .buildHttpApi()
            .websocket("/ws", app)
                .buildWebsocket();

    }

}

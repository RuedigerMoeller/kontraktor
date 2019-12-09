package de.nustaq.dartserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.*;

public class DartServer extends Actor<DartServer> {

    public static final int NUM_SESSOIN_THREADS = 4;
    public static final int SESSION_TIMEOUT_MINUTES = 10;
    // threads to dispatch session onto
    private Scheduler clientThreads[];
    private Random rand = new Random();

    @Local
    public void init() {
        clientThreads = new Scheduler[NUM_SESSOIN_THREADS];
        IntStream.range(0,NUM_SESSOIN_THREADS)
            .forEach( i -> clientThreads[i] = new SimpleScheduler(10000, true /*Important!*/ ));
    }

    public IPromise test(String arg0 ) {
        return resolve("hello "+arg0);
    }

    public IPromise test1( String arg0, Callback cb ) {
        delayed(1000, () -> cb.pipe(arg0+" ->1"));
        delayed(2000, () -> cb.pipe(arg0+" ->2"));
        delayed(3000, () -> cb.complete());
        return resolve("hello callback "+arg0);
    }

    public IPromise login(String email, String pwd, Callback events ) {
        if ( "".equals(email.trim()) ) {
            return reject("empty email");
        }
        Promise p = new Promise();
        if ( pwd.equals( "qweqwe") ) {
            DartSession session = AsActor(
                DartSession.class,
                // randomly distribute session actors among clientThreads
                clientThreads[rand.nextInt(clientThreads.length)]
            );
            Map user = new HashMap();
            user.put("name","me");
            session.init(user,self(),events);
            p.resolve(new LoginData().session(session).user(user)); // == new Promise(session)
        } else {
            p.reject("wrong user or password");
        }
        return p;
    }

    public static void main(String[] args) throws InterruptedException {

        DartServer app = AsActor(DartServer.class);
        app.init();

        Class CLAZZES[] = {
        };

        Http4K.Build( "0.0.0.0", 8087)
            .fileRoot( "img","./run/data/img")
            .httpAPI("/api", app) // could also be websocket based (see IntrinsicReactJSX github project)
                .coding(new Coding(SerializerType.JsonNoRef, CLAZZES))
                .setSessionTimeout(TimeUnit.MINUTES.toMillis(SESSION_TIMEOUT_MINUTES))
                .buildHttpApi()
            .build();

    }


}

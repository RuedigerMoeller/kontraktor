package sample.httpjs;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by ruedi on 29/05/15.
 *
 * Example Single Page Server with actors talking to JavaScript.
 *
 * This class provides the stateless server API exposed to JS. By calling login,
 * the client can authenticate and instantiate a dedicated SessionActor instance
 * which then expose per-client api and server side state. (see ../../web/index.html)
 *
 */
public class MyHttpApp extends Actor<MyHttpApp> {

    public static final int CLIENT_QSIZE = 1000;

    Scheduler clientThreads[];
    int sessionCount = 0;

    public IPromise<String> getServerTime() {
        return new Promise<>(new Date().toString());
    }

    public void init() {
        clientThreads = new Scheduler[]{
            new SimpleScheduler(CLIENT_QSIZE), // only one session processor thread should be sufficient for most apps.
        };
        Thread.currentThread().setName("MyHttpApp Dispatcher");
    }

    public IPromise<MyHttpAppSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session and assign it a random scheduler (~thread). Note that with async nonblocking style
            // one thread will be sufficient most of the time. For very computing intensive apps increase clientThreads to like 2-4
            MyHttpAppSession sess = AsActor(MyHttpAppSession.class,clientThreads[((int) (Math.random() * clientThreads.length))]);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), Arrays.asList("procrastinize", "drink coffee", "code", "play the piano", "ignore *") );
            result.resolve(sess);
            sessionCount++;
        }
        return result;
    }

    public IPromise<Integer> getNumSessions() {
        return resolve(sessionCount);
    }

    public void voidMethodForBenchmark() {}

    public IPromise promiseMethodForBenchmark(int num) {
        return new Promise<>(num);
    }

    @Local
    public void clientClosed(MyHttpAppSession session) {
        sessionCount--;
        System.out.println("client closed "+session);
    }

}

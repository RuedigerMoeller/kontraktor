package sample.httpjs;

import org.nustaq.kontraktor.*;
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

    Scheduler clientThreads[] = {
        new SimpleScheduler(1000) // only one session processor thread for now.
    };

    public IPromise<String> getServerTime() {
        return new Promise<>(new Date().toString());
    }

    public IPromise<MyHttpAppSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session and assign it a random scheduler (~thread). Note that with async nonblocking style
            // one thread will be sufficient. For very computing intensive apps increase clientThreads to like 2-4
            MyHttpAppSession sess = AsActor(MyHttpAppSession.class,clientThreads[((int) (Math.random() * clientThreads.length))]);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), Arrays.asList("procrastinize", "drink coffee", "code", "play the piano", "ignore *") );
            result.resolve(sess);
        }
        return result;
    }

    public IPromise<Integer> getNumSessions() {
        return resolve(clientThreads[0].getNumActors());
    }

    public void clientClosed(MyHttpAppSession session) {
        System.out.println("client closed "+session);
    }

}

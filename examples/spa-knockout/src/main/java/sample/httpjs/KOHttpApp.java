package sample.httpjs;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 29/05/15.
 *
 * Example Single Page Server with actors talking to JavaScript.
 *
 * This class provides the stateless server API exposed to JS. By calling login,
 * the client can authenticate and instantiate a dedicated SessionActor instance
 * which then exposes a per-client api and server side state by creating a session actor for each client.
 *
 */
public class KOHttpApp extends Actor<KOHttpApp> {

    public static final int CLIENT_QSIZE = 1000;

    Scheduler clientThreads[] = {
        new SimpleScheduler(CLIENT_QSIZE) // only one session processor thread should be sufficient for most apps.
    };

    HashMap<KOAppSession,Callback> chatSubscription = new HashMap<>();
    List<KOPushEvent> history = new ArrayList<>();

    public IPromise<KOAppSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session and assign it a random scheduler (~thread). Note that with async nonblocking style
            // one thread will be sufficient most of the time. For very computing intensive apps increase clientThreads to like 2-4
            KOAppSession sess = AsActor(KOAppSession.class,clientThreads[((int) (Math.random() * clientThreads.length))]);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), Arrays.asList("procrastinize", "drink coffee", "code", "play the piano", "ignore *"), user );
            result.resolve(sess);
        }
        return result;
    }

    @Local
    public void subscribeChat( KOAppSession session, Callback<KOPushEvent> cb ) {
        chatSubscription.put(session,cb);
        history.forEach(pushevent -> cb.stream(pushevent));
        cb.stream( new KOPushEvent().numSessions(clientThreads[0].getNumActors()) );
        broadCastSessions();
    }

    @Local
    public void broadCastChatMsg( String from, String msg ) {
        KOPushEvent ev = new KOPushEvent().msg(msg).msgFrom(from);
        history.add(0, ev);
        while ( history.size() > 10 )
            history.remove(10);
        chatSubscription.values().forEach( callback -> callback.stream(ev) );
    }

    void broadCastSessions() {
        KOPushEvent ev = new KOPushEvent().numSessions(chatSubscription.size());
        chatSubscription.values().forEach( callback -> callback.stream(ev) );
    }

    public IPromise<Integer> getNumSessions() {
        return resolve(chatSubscription.size());
    }

    @Local
    public void clientClosed(KOAppSession session) {
        System.out.println("client closed "+session+" chatSubscriptions:"+chatSubscription.size());
        chatSubscription.remove(session);
        broadCastSessions();
    }

}

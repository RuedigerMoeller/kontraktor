package org.nustaq.kontraktor.examples.statefulserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.util.HashMap;

/**
 * Created by ruedi on 23.08.2014.
 */
public class ServerSession extends Actor<ServerSession> {

    public static final int PING_INTERVAL = 3000;
    volatile String user;
    StatefulServer server;
    long lastPing = System.currentTimeMillis();

    public void $init(String user, StatefulServer server) {
        this.user = user;
        this.server = server;
        self().$checkHeartbeat();
    }

    public void $logout() {
        server.$clientTerminated(self());
        self().$close();
        self().$stop();
    }

    public void $ping() {
        lastPing = System.currentTimeMillis();
    }

    public void $checkHeartbeat() {
        if ( System.currentTimeMillis() - lastPing > 2 * PING_INTERVAL ) {
            $logout();
        }
        delayed(PING_INTERVAL, () -> {
            if ( ! isStopped() )
                self().$checkHeartbeat();
        });
    }

    // return user async. Will work on remote ref
    public Future<String> $getUser() {
        return new Promise<>(user);
    }

    // return user synchronouse. Warning: this won't work on remote ref
    @CallerSideMethod public String getUser() {
        return getActor().user;
    }

    public Future<HashMap<String,Character>> $dummyWork() {
        HashMap result = new HashMap();
        user.chars().forEach( (ch) -> result.put( ""+(char)ch, ch ) );
        return new Promise<>(result);
    }
}

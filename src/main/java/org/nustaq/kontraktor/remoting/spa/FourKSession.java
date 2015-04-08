package org.nustaq.kontraktor.remoting.spa;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.RemotableActor;

import java.util.Date;

/**
 * Created by ruedi on 07/04/15.
 */
@Local
public class FourKSession<SERVER extends FourK,SESSION extends FourKSession> extends Actor<SESSION> implements RemotableActor {

    protected long creationTime = System.currentTimeMillis();
    volatile protected long lastHB = System.currentTimeMillis();
    protected String sessionId;
    protected SERVER app;

    public void $initFromServer(String sessionId, SERVER spaServer, Object resultFromIsLoginValid) {
        this.app = spaServer;
        this.sessionId = sessionId;
        setThrowExWhenBlocked(true);
    }

    public IPromise<String> $getId() {
        return new Promise<>(sessionId);
    }
    public IPromise<String> $getCreationTime() {
        return new Promise<>(new Date(creationTime).toString() ); //JS no long :(
    }

    public void $heartBeat() {
        lastHB = System.currentTimeMillis();
    }

    @Override
    @Local
    public void $hasBeenUnpublished() {
        app.$clientTerminated(self()).then(() -> self().$stop());
        self().$close();
    }

    @CallerSideMethod @Local
    public long getLastHB() {
        return lastHB;
    }

}
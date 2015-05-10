package org.nustaq.kontraktor.remoting.spa;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.util.Log;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ruedi on 07/04/15.
 */
@Local
public class FourKSession<SERVER extends FourK,SESSION extends FourKSession> extends Actor<SESSION> implements RemotedActor {

    protected long creationTime = System.currentTimeMillis();
    volatile protected long lastHB = System.currentTimeMillis();
    volatile protected String sessionId;
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
        Log.Info(this, "Hearbeat from " + sessionId);
        getActor().lastHB = System.currentTimeMillis();
    }

    /**
     * called once a websocket connection terminates/closes. The snapshot
     * contains all active published <actors,id> mappings to enable reconnect / long poll
     * fallback.
     */
    @Override @Local
    public void $hasBeenUnpublished() {
        app.$clientTerminated(self()).then(() -> {
            if (!app.isStickySessions())
                self().$close();
        });
    }

    @CallerSideMethod @Local
    public long getLastHB() {
        return getActor().lastHB;
    }

    @CallerSideMethod @Local
    public String getSessionId() {
        return getActor().sessionId;
    }

    //// internal.

    @CallerSideMethod @Local
    public RemoteRegistry __getRegistry() {
        ConcurrentLinkedQueue connections = getActor().__connections;
        if ( connections == null )
            return null;
        return (RemoteRegistry) connections.peek();
    }

    // for longpolling connection is set explictely
    @CallerSideMethod @Local
    public void __setRegistry(RemoteRegistry registry) {
        __addRemoteConnection(registry);
        if ( __connections.size() > 1 ) {
            Log.Lg.error(this,null,"more than one connection on FourKSession");
        }
    }


}
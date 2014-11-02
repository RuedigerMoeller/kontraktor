package org.nustaq.machweb;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.RemotableActor;

import java.util.Date;

/**
 * Created by ruedi on 01.11.14.
 */
public class MachWebSession<SERVER extends MachWeb,SESSION extends MachWebSession> extends Actor<SESSION> implements RemotableActor {

    protected long creationTime = System.currentTimeMillis();
    protected String sessionId;
    protected SERVER app;

    public void $initFromServer(String sessionId, SERVER server ) {
        this.app = server;
        this.sessionId = sessionId;
        setThrowExWhenBlocked(true);
    }

    public Future<String> $getId() {
        return new Promise<>(sessionId);
    }
    public Future<String> $getCreationTime() {
        return new Promise<>(new Date(creationTime).toString() ); //JS no long :(
    }

    @Override
    @Local
    public void $hasBeenUnpublished() {
        app.$clientTerminated(self()).then(() -> self().$stop());
    }

}

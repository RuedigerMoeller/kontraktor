package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

/**
 * Created by ruedi on 20.06.17.
 */
public abstract class BasicWebSessionActor<T extends BasicWebSessionActor> extends Actor<T> implements RemotedActor {

    protected BasicWebAppActor app;
    protected String user;

    /**
     * this can be null in case of websocket connections FIXME
     */
    protected String sessionId;

    public void init(BasicWebAppActor app, String user, String sessionId) {
        this.app = app;
        this.user = user;
        this.sessionId = sessionId;
    }

    @Override
    public void hasBeenUnpublished() {
        app.notifySessionEnd(self());
        ISessionStorage storage = app._getSessionStorage();
        persistSession(storage);
    }

    /**
     * persist session state for resurrection later on, do nothing if resurrection should not be supported
     * @param storage
     */
    protected abstract void persistSession(ISessionStorage storage);

}

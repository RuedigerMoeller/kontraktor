package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

/**
 * Created by ruedi on 20.06.17.
 */
public abstract class BasicWebSessionActor<T extends BasicWebSessionActor> extends Actor<T> implements RemotedActor {

    protected BasicWebAppActor app;
    protected String userKey;

    /**
     * this can be null in case of websocket connections FIXME
     */
    protected String sessionId;

    @Local
    public IPromise init(BasicWebAppActor app, BasicAuthenticationResult user, String sessionId) {
        this.app = app;
        this.userKey = user.getUserKey();
        this.sessionId = sessionId;
        return loadSessionData(sessionId,getSessionStorage());
    }

    @CallerSideMethod
    public String getSessionId() {
        return getActor().sessionId;
    }

    @CallerSideMethod
    public String getUserKey() {
        return getActor().userKey;
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {
        app.notifySessionEnd(self());
        ISessionStorage storage = app.getSessionStorage();
        persistSessionData(sessionId, storage);
    }

    /**
     * persist session state for resurrection later on, do nothing if resurrection should not be supported
     * @param storage
     */
    protected abstract void persistSessionData(String sessionId, ISessionStorage storage);
    /**
     * laod session state after resurrection. the object returned is sent along the login response to the client
     * so an extra intialization call round trip can be omitted.
     *
     * @param storage
     */
    protected abstract IPromise loadSessionData(String sessionId, ISessionStorage storage);

    protected ISessionStorage getSessionStorage() {
        return app.getSessionStorage();
    }
}

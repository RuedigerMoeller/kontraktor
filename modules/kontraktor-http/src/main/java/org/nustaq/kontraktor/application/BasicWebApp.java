package org.nustaq.kontraktor.application;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 20.06.17.
 */
public class BasicWebApp<T extends BasicWebApp,C extends BasicWebAppConfig> extends Actor<T> implements SessionResurrector {

    protected Scheduler[] sessionThreads;
    protected IBasicSessionStorage sessionStorage;

    @Local
    public void init(C config) {
        int numSessionThreads = config.getNumSessionThreads();
        sessionThreads = new Scheduler[numSessionThreads];
        for (int i = 0; i < numSessionThreads; i++ ) {
            sessionThreads[i] = new SimpleScheduler(10_000, true);
        }
    }

    public IPromise<BasicWebAppSession> login(String user, String pw) {
        //TODO: verify user, pw ASYNC !
        if ( "admin".equals(user)) {
            return new Promise<>(null,"hehe");
        }
        String sessionId = connection.get() != null ? connection.get().getSocketRef().getConnectionIdentifier() : null;
        BasicWebAppSession sess = createSession(user,sessionId);
        return new Promise<>(sess);
    }

    protected BasicWebAppSession createSession(String user, String sessionId) {
        BasicWebAppSession sess = Actors.AsActor(getSessionClazz(),sessionThreads[(int) (Math.random()*sessionThreads.length)]);
        sess.init(self(),user,sessionId);
        return sess;
    }

    protected Class<BasicWebAppSession> getSessionClazz() {
        return BasicWebAppSession.class;
    }

    /**
     * just a notification
     * @param sessionId
     */
    @Override
    public void restoreRemoteRefConnection(String sessionId) {
        Log.Info(this,"reanimating "+ sessionId);
    }

    /**
     * An existing spa client made a request after being inactive for a long time.
     *
     * Your mission: reconstruct a session actor and state (e.g. from persistence) so it can continue or
     * return null and handle "session expired" correctly at client side.
     *
     * session resurrection is called using await => hurry up
     *
     * @param sessionId
     * @param remoteRefId
     * @return resurrected BasicWebAppSession
     */
    @Override
    public IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
        if ( sessionStorage == null )
            return resolve(null);
        Promise res = new Promise();
        sessionStorage.getUserFromSessionId(sessionId).then( (user,err) -> {
            if ( user == null )
                res.resolve(null);
            else
                res.resolve(createSession((String) user, sessionId));
        });
        return res;
    }
}

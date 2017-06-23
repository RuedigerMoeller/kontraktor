package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 20.06.17.
 */
public abstract class BasicWebAppActor<T extends BasicWebAppActor,C extends BasicWebAppConfig> extends Actor<T> implements SessionResurrector {

    protected Scheduler[] sessionThreads;
    protected ISessionStorage sessionStorage;

    @Local
    public void init(C config) {
        int numSessionThreads = config.getNumSessionThreads();
        sessionThreads = new Scheduler[numSessionThreads];
        for (int i = 0; i < numSessionThreads; i++ ) {
            sessionThreads[i] = new SimpleScheduler(10_000, true);
        }
    }

    public IPromise<T> login(String user, String pw, String jwt) {
        Promise res = new Promise();
        verifyCredentials(user,pw,jwt).then( (authres,err) -> {
            if ( err != null ) {
                res.reject(err);
            } else {
                String sessionId = connection.get() != null ? connection.get().getSocketRef().getConnectionIdentifier() : null;
                BasicWebSessionActor sess = createSession(user,sessionId);
                res.resolve(sess);
            }
        });
        return res;
    }

    protected abstract IPromise<String> verifyCredentials(String s, String pw, String jwt);/* {
        //TODO: verify user, pw ASYNC !
        if ( "admin".equals(user)) {
            return reject("authentication failed");
        }
        return resolve("logged in");
    }*/

    protected BasicWebSessionActor createSession(String user, String sessionId) {
        BasicWebSessionActor sess = Actors.AsActor((Class<BasicWebSessionActor>) getSessionClazz(),sessionThreads[(int) (Math.random()*sessionThreads.length)]);
        sess.init(self(),user,sessionId);
        return sess;
    }

    protected abstract Class getSessionClazz(); /* {
        return BasicWebSessionActor.class;
    }*/

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
     * @return resurrected BasicWebSessionActor
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

    public void notifySessionEnd(BasicWebSessionActor session) {

    }

    @CallerSideMethod
    public ISessionStorage _getSessionStorage() {
        return getActor().sessionStorage;
    }

}

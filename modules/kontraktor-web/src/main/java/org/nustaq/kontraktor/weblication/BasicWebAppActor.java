package org.nustaq.kontraktor.weblication;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.model.DefaultSessionStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 20.06.17.
 */
public abstract class BasicWebAppActor<T extends BasicWebAppActor,C extends BasicWebAppConfig> extends Actor<T> implements SessionResurrector {

    public static String WEBAPP_DIR = "./src/main/web/client";
    public static String BASH_EXEC = "/usr/bin/bash";
    public static String BABEL_SERVER_JS_PATH = "./node_modules/babelserver/babelserver.js";

    protected Scheduler[] sessionThreads;
    protected ISessionStorage sessionStorage;
    protected Map<String,BasicWebSessionActor> sessions;

    @Local
    public void init(C config) {
        int numSessionThreads = config.getNumSessionThreads();
        sessionThreads = new Scheduler[numSessionThreads];
        for (int i = 0; i < numSessionThreads; i++ ) {
            sessionThreads[i] = new SimpleScheduler(10_000, true);
        }
        sessionStorage = createSessionStorage(config);
        sessions = new HashMap<>();
    }

    protected ISessionStorage createSessionStorage(C config) {
        DefaultSessionStorage defaultSessionStorage = AsActor(DefaultSessionStorage.class);
        defaultSessionStorage.init(new DefaultSessionStorage.Config()).await(30_000);
        return defaultSessionStorage;
    }

    /**
     * returns an array of [session actorproxy, userdata]
     * @param user
     * @param pw
     * @param jwt
     * @return array [session actor proxy, authenticationresult]
     */
    @Remoted
    public IPromise<Object[]> login(String user, String pw, String jwt) {
        Promise res = new Promise();
        ConnectionRegistry remoteConnection = connection.get();
        getCredentials(user,pw,jwt).then( (authres, err) -> {
            if ( err != null ) {
                res.reject(err);
            } else {
                String sessionId = remoteConnection != null ? remoteConnection.getSocketRef().getConnectionIdentifier() : null;
                getSession(user,sessionId, authres ).then( (sess,serr) -> {
                    if ( sess != null ) {
                        if ( sessionId != null && sessionStorage != null ) {
                            sessionStorage.putUserAtSessionId(sessionId,user);
                        }
                        res.resolve(new Object[]{sess,authres});
                    } else
                        res.complete(sess,serr);
                });
            }
        });
        return res;
    }

    /**
     * does a lookup for a user record using 'user' as key. Compares pw with 'pwd' of user record if found
     *
     * @param pw
     * @param jwt
     * @return
     */
    protected IPromise<BasicAuthenticationResult> getCredentials(String user, String pw, String jwt) {
        Promise p = new Promise();
        sessionStorage.getUser(user).then( (rec, err) -> {
            if ( rec == null ) {
                p.reject("wrong user or password");
            } else {
                if ( pw.equals(rec.getString("pwd" ) ) ) {
                    if ( ! rec.getBool("verified") ) {
                        // e.g. email verification etc.
                        p.reject("account not verified");
                    } else
                        p.resolve( new BasicAuthenticationResult().userName(rec.getKey()) );
                }
            }
        });
        return p;
    }

    protected IPromise<BasicWebSessionActor> getSessionForReanimation(String user, String sessionId) {
        return getSession(user,sessionId,new BasicAuthenticationResult().userName(user));
    }

    protected IPromise<BasicWebSessionActor> getSession(String user, String sessionId, BasicAuthenticationResult authenticationResult) {
        // session sharing has major implications on resurrection and callback / subscription handling
//        BasicWebSessionActor sess = sessions.get(user);
//        if ( sess != null && ! sess.isStopped() ) {
//            return sess;
//        }
        return createSession(user,sessionId,authenticationResult);
    }

    protected IPromise<BasicWebSessionActor> createSession(String user, String sessionId, BasicAuthenticationResult authenticationResult) {
        Promise p = new Promise();
        BasicWebSessionActor sess = AsActor((Class<BasicWebSessionActor>) getSessionClazz(),sessionThreads[(int) (Math.random()*sessionThreads.length)]);
        sess.init(self(),authenticationResult,sessionId).then( (res,err) -> {
            if ( err == null ) {
                sessions.put(sessionId,sess);
                authenticationResult.initialData(res);
                p.resolve(sess);
            } else
                p.reject(err);
        });
        return p;
    }

    protected abstract Class getSessionClazz(); /* {
        return BasicWebSessionActor.class;
    }*/

    /**
     * An existing spa client made a request after being inactive for a long time.
     *
     * mission: reconstruct a session actor and load its state (e.g. from persistence) so it can continue or
     * return null and handle "session expired" correctly at client side. Note there is a default implementation
     *
     * session resurrection is called using await => hurry up
     *
     * return new Promise(null) in order to bypass session reanimation default implementation
     *
     * @param sessionId
     * @param remoteRefId
     * @return
     *
     * @param sessionId
     * @param remoteRefId
     * @return resurrected BasicWebSessionActor
     */
    @Override @Local
    public IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
        if ( sessionStorage == null )
            return resolve(null);
        Promise res = new Promise();
        sessionStorage.getUserFromSessionId(sessionId).then( (user, err) -> {
            if ( user == null )
                res.resolve(null);
            else {
                getSessionForReanimation(user, sessionId).then( res );
            }
        });
        return res;
    }

    @Local
    public void notifySessionEnd(BasicWebSessionActor session) {
        sessions.remove(session.getSessionId());
        Log.Info(this, "session timed out "+session.getSessionId()+" "+session.getUserKey()+" sessions:"+sessions.size());
    }

    @CallerSideMethod
    public ISessionStorage getSessionStorage() {
        return getActor().sessionStorage;
    }

    /**
     * reply a request catched by interceptor, note this is server dependent and bound to undertow.
     * for servlet containers, just override KontraktorServlet methods
     * @param exchange
     */
    public void handleDirectRequest(HttpServerExchange exchange) {
        Log.Info(this,"direct request received "+exchange);
        getDirectRequestResponse(exchange.getRequestPath()).then( (s,err) -> {
            exchange.setResponseCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
            exchange.getResponseSender().send(s == null  ? ""+err : s );
        });
    }

    /**
     * simplified, override handleDirectRequest() for full control+access to http header and response type
     * @param path
     * @return
     */
    protected IPromise<String> getDirectRequestResponse(String path) {
        return new Promise("Hey there "+path);
    }

}

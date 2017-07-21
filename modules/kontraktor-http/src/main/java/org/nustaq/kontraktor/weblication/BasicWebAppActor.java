package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.babel.BrowseriBabelify;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        defaultSessionStorage.init(new DefaultSessionStorage.Config());
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
        RemoteConnection remoteConnection = connection.get();
        getCredentials(user,pw,jwt).then( (authres, err) -> {
            if ( err != null ) {
                res.reject(err);
            } else {
                String sessionId = remoteConnection != null ? remoteConnection.getSocketRef().getConnectionIdentifier() : null;
                BasicWebSessionActor sess = createSession(user,sessionId, authres );
                if ( sessionId != null && sessionStorage != null ) {
                    sessionStorage.putSessionId(sessionId,user);
                }
                res.resolve(new Object[]{sess,authres});
            }
        });
        return res;
    }

    /**
     * does a lookup for a usere record using 'user' as key. Compares pw with 'pwd' of user record if found
     *
     * @param pw
     * @param jwt
     * @return
     */
    protected IPromise<BasicAuthenticationResult> getCredentials(String user, String pw, String jwt) {
        Promise p = new Promise();
        sessionStorage.getUserRecord(user).then( (rec,err) -> {
           if ( rec == null ) {
               p.reject("wrong user or password");
           } else {
               if ( pw.equals(rec.getString("pwd" ) ) ) {
                   p.resolve( new BasicAuthenticationResult().userName(rec.getKey()) );
               }
           }
        });
        return p;
    }

    protected BasicWebSessionActor createSessionForReanimation(String user, String sessionId) {
        BasicWebSessionActor session = createSession(user, sessionId, new BasicAuthenticationResult().userName(user));
        return session;
    }

    protected BasicWebSessionActor createSession(String user, String sessionId, BasicAuthenticationResult authenticationResult) {
        BasicWebSessionActor sess = Actors.AsActor((Class<BasicWebSessionActor>) getSessionClazz(),sessionThreads[(int) (Math.random()*sessionThreads.length)]);
        sess.init(self(),authenticationResult,sessionId);
        sessions.put(user,sess);
        return sess;
    }

    protected abstract Class getSessionClazz(); /* {
        return BasicWebSessionActor.class;
    }*/

    /**
     * just a notification
     * @param sessionId
     */
    @Override @Local
    public void restoreRemoteRefConnection(String sessionId) {
        Log.Info(this,"try reanimating "+ sessionId);
    }

    /**
     * An existing spa client made a request after being inactive for a long time.
     *
     * Your mission: reconstruct a session actor and state (e.g. from persistence) so it can continue or
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
        sessionStorage.getUserKeyFromSessionId(sessionId).then( (user, err) -> {
            if ( user == null )
                res.resolve(null);
            else {
                res.resolve(createSessionForReanimation( user, sessionId ));
            }
        });
        return res;
    }

    @Local
    public void notifySessionEnd(BasicWebSessionActor session) {
        sessions.remove(session._getUserKey());
        Log.Info(this, "session timed out "+session._getSessionId()+" "+session._getUserKey()+" sessions:"+sessions.size());
    }

    @CallerSideMethod
    public ISessionStorage _getSessionStorage() {
        return getActor().sessionStorage;
    }

    /**
     * util to startup babel/browserify daemon
     * @return true if successful
     */
    public static boolean runNodify() {
        try {
            BrowseriBabelify.get();
        } catch (Exception ex) {
            Log.Warn(BasicWebAppActor.class,"babelserver not running .. try starting");
            boolean isWindows = System.getProperty("os.name","linux").toLowerCase().indexOf("windows") >= 0;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (isWindows) {
                    processBuilder.command("cmd.exe", "/c", "node "+ BABEL_SERVER_JS_PATH);
                } else {
                    String bash = BASH_EXEC;
                    if ( !new File(bash).exists() ) {
                        bash = "/bin/bash";
                    }
                    processBuilder.command(bash, "-c", "node "+ BABEL_SERVER_JS_PATH);
                }
                processBuilder.directory(new File(WEBAPP_DIR));
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                for ( int i = 0; i < 8; i++ ) {
                    Thread.sleep(500);
                    System.out.print('.');
                    try {
                        BrowseriBabelify.get();
                        break;
                    } catch (Exception e) {
                        if ( i==3 ) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }
        return true;
    }

}

package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.babel.BrowseriBabelify;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.util.Log;

import java.io.File;

/**
 * Created by ruedi on 20.06.17.
 */
public abstract class BasicWebAppActor<T extends BasicWebAppActor,C extends BasicWebAppConfig> extends Actor<T> implements SessionResurrector {

    public static String WEBAPP_DIR = "./src/main/web/client";
    public static String BASH_EXEC = "/usr/bin/bash";
    public static String BABEL_SERVER_JS_PATH = "./node_modules/babelserver/babelserver.js";
    protected Scheduler[] sessionThreads;
    protected ISessionStorage sessionStorage;

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
                for ( int i = 0; i < 4; i++ ) {
                    Thread.sleep(500);
                    System.out.print('.');
                    try {
                        BrowseriBabelify.get();
                        continue;
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

    @Local
    public void init(C config) {
        int numSessionThreads = config.getNumSessionThreads();
        sessionThreads = new Scheduler[numSessionThreads];
        for (int i = 0; i < numSessionThreads; i++ ) {
            sessionThreads[i] = new SimpleScheduler(10_000, true);
        }
        sessionStorage = createSessionStorage();
    }

    protected ISessionStorage createSessionStorage() {
        return AsActor(DefaultSessionStorage.class);
    }

    /**
     * returns an array of [session actorproxy, userdata]
     * @param user
     * @param pw
     * @param jwt
     * @return
     */
    public IPromise<Object[]> login(String user, String pw, String jwt) {
        Promise res = new Promise();
        getCredentials(user,pw,jwt).then( (authres, err) -> {
            if ( err != null ) {
                res.reject(err);
            } else {
                String sessionId = connection.get() != null ? connection.get().getSocketRef().getConnectionIdentifier() : null;
                BasicWebSessionActor sess = createSession(user,sessionId, authres );
                res.resolve(new Object[]{sess,authres});
            }
        });
        return res;
    }

    /**
     * return successmessage or reject with an error message
     * @param s
     * @param pw
     * @param jwt
     * @return
     */
    protected IPromise<BasicAuthenticationResult> getCredentials(String user, String pw, String jwt) {
        Promise p = new Promise();
        sessionStorage.getUserRecord(user).then( (rec,err) -> {
           if ( rec == null ) {
               p.reject("unknown userkey");
           }
        });
        return p;
    }

    protected BasicWebSessionActor createSession(String user, String sessionId, BasicAuthenticationResult authenticationResult) {
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
        sessionStorage.getUserKeyFromSessionId(sessionId).then( (user, err) -> {
            if ( user == null )
                res.resolve(null);
            else {
                res.resolve(createSession((String) user, sessionId, null ));
            }
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

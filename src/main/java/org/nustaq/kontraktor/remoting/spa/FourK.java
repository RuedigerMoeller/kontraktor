package org.nustaq.kontraktor.remoting.spa;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.RemoteRegistry;
import org.nustaq.kontraktor.remoting.base.ActorServerConnection;
import org.nustaq.kontraktor.remoting.javascript.DependencyResolver;
import org.nustaq.kontraktor.remoting.javascript.minbingen.MB2JS;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Local
public abstract class FourK<SERVER extends Actor,SESSION extends FourKSession> extends Actor<SERVER> {

    public static long SESSION_FULL_TIMEOUT = 1000 * 60 * 60; // give one hour for reconnect
    public static int SESSION_HB_TIMEOUT = 30000; // client side ping timeout trigger

    public static String StripDoubleSeps(String s) {
        int len;
        do {
            len = s.length();
            s = s.replace("\\","/");
            s = s.replace("//","/");
        } while ( s.length() != len );
        return s;
    }

    protected Map<String, SESSION> sessions;
    protected long sessionIdCounter = 1;
    protected Scheduler clientScheduler; // set of threads processing client requests
    protected AppConf conf;
    protected Map<String,DependencyResolver> loader = new HashMap<>();
    protected HashMap<String,String> shortClassNameMapping;
    protected String appRootDir = "./";
    volatile protected boolean stickySessions;

    /**
     *
     * @param clientScheduler
     * @param stickySessions if sticky session is true, sessions will expire by timeout only. A
     *                       disconnect will unpublish the session actor, however it can be reconnected
     *                       using "reconnectSession" later on.
     */
    @Local
    public void $init(Scheduler clientScheduler, boolean stickySessions) {
        this.sessions = new HashMap<>();
        this.clientScheduler = clientScheduler;
        this.stickySessions = stickySessions;
        delayed( 1000, () -> $checkSessions() );
    }

    @Local
    public void $checkSessions() {
        // fixme: add worst case session timeout in case to much connections
        long now = System.currentTimeMillis();
        List toRemove = new ArrayList();
        sessions.values().forEach(session -> {
            if (!session.isPublished()) {
                if (now - session.getLastHB() > SESSION_FULL_TIMEOUT) {
                    Log.Info(this, "full timeout. deleting session " + session.getSessionId());
                    toRemove.add(session.getSessionId());
                    session.__getRegistry().setIsObsolete(true);
                }
            }
        });

        toRemove.forEach(sessionKey -> sessions.remove(sessionKey));
        delayed(2000, () -> $checkSessions());
    }


    @CallerSideMethod
    public boolean isStickySessions() {
        return ((FourK)getActor()).stickySessions;
    }

    /**
     * to avoid the need for anonymous clients to create a websocket connection prior to login,
     * this is exposed as a webservice and is called using $.get(). The Id returned then can be
     * used to obtain a valid session id for the websocket connection.
     *
     * @param user
     * @param pwd
     * @return
     */
    public IPromise<String> $authenticate(String user, String pwd) {
        Promise p = new Promise();
        isLoginValid(user, pwd).then(
            (result, e) -> {
                if (result != null) {
                    String sessionId = Long.toHexString((long) (Math.random() * Long.MAX_VALUE)) + "" + sessionIdCounter++; // FIXME: use strings
                    SESSION newSession = createSessionActor(sessionId, clientScheduler, result);
                    newSession.$initFromServer(sessionId, (FourK) self(), result);
                    sessions.put(sessionId, newSession);
                    newSession.setThrowExWhenBlocked(true);
                    p.complete(sessionId, null);
                } else {
                    p.complete(null, "authentication failure");
                }
            });
        return p;
    }

    public IPromise<SESSION> $getSession(String id) {
        System.out.println("getSession registry " + Actor.registry.get());
        SESSION session = sessions.get(id);
        return new Promise<>(session);
    }

    public IPromise<Boolean> $reconnectSession(String id, int sequence) {
        Log.Info(this, "try reconnect session " + id);
        RemoteRegistry newRemoteRefRegistry = Actor.registry.get(); // caveat NOT available in callbacks
        // FIXME: protect against brute force
        SESSION session = sessions.get(id);
        if ( session != null ) {
            Promise res = new Promise<>();
            serialOn( session, finSignal -> {
                try {
                    if ( newRemoteRefRegistry != null ) {
                        Log.Info(this, "reconnection successful. session " + id);
                        ((ActorServerConnection)newRemoteRefRegistry).handOverTo(session.__getRegistry());
                        System.out.println("session remote con size "+session.__connections.size());
                        res.resolve(true);
                    } else {
                        Log.Info(this, "reconnection failed, no registry set " + id);
                        res.resolve(false);
                    }
                } finally {
                    finSignal.complete();
                }
            });
            return res;
        }
        Log.Info(this, "reconnection session failed " + id);
        return new Promise(false);
    }

    /**
     * called upon disconnect from a client.
     * by default, this also destroys the session. If sticky sessions are enabled,
     * sessionactor is unpublished, but can be reactivated using reconnectSession
     *
     * @param session
     * @return
     */
    @Local
    public IPromise $clientTerminated(SESSION session) {
        Promise p = new Promise();
        session.$getId().then((id, err) -> {
            if (!stickySessions)
                sessions.remove(id);
            p.complete();
        });
        return p;
    }

    /**
     * return null if not a user, else return a user-object which is then passed to
     * session object
     * @param user
     * @param pwd
     * @return - null or user object passed to sessionactor $init
     */
    abstract protected IPromise<Object> isLoginValid(String user, String pwd);
    abstract protected SESSION createSessionActor(String sessionId, Scheduler clientScheduler, Object resultFromIsLoginValid);

    @Override @Local
    public IPromise<Monitorable[]> $getSubMonitorables() {
        Monitorable[] mon = new Monitorable[sessions.size()];
        sessions.values().toArray(mon);
        return new Promise<>(mon);
    }

    @Local
    public IPromise<AppConf> $getConf() {
        return new Promise<>(conf);
    }

    /**
     * startup server + map some files for development
     * @throws Exception
     */
    @Local
    public IPromise<Map<String,DependencyResolver>> $main(String appRootDir) {
        try {
            this.appRootDir = appRootDir;
            // patch conf from args
            conf = (AppConf) new Kson().supportJSon(false).readObject(getFile("conf.kson"), AppConf.class.getName());


            getFile("tmp").mkdirs();
            generateRemoteStubs(conf,getClass().getPackage().getName());

            shortClassNameMapping = new HashMap<>();
            if ( getFile("tmp/name-map.kson").exists() )
                shortClassNameMapping = (HashMap<String, String>) new Kson().supportJSon(false).readObject(getFile("tmp/name-map.kson"), HashMap.class);

            SimpleScheduler scheduler = new SimpleScheduler(conf.clientQSize); // fixme: add more / auto-unblock

            conf.getAppMappings().forEach( (k,v) -> {
                // install handler to automatically search and bundle jslibs + template snippets
                loader.put( k, new DependencyResolver( appRootDir, v, conf.componentPath ) );
            });

            $init(scheduler, conf.stickySessions);

            // fixme: slowish
            BiFunction<Actor,String,Boolean> methodSecurity = (actor, method) -> {
                if ( conf.allowedMethods != null ) {
                    HashSet<String> methods = conf.allowedMethods.get(actor.getActor().getClass().getSimpleName());
                    if ( methods != null )
                        return methods.contains(method);
                }
                if ( conf.forbiddenMethods != null ) {
                    HashSet<String> methods = conf.forbiddenMethods.get(actor.getActor().getClass().getSimpleName());
                    if (methods == null)
                        return ! methods.contains(method);
                }
                return true;
            };

        } catch (Exception e) {
            e.printStackTrace();
            return new Promise<>(null, e);
        }
        return new Promise<>(loader);
    }

    private File getFile(String s) {
        return new File(appRootDir+File.separator+s);
    }

    protected void generateRemoteStubs(AppConf appconf, String serverpackage) throws Exception {
        String genBase = serverpackage;
        if ( appconf.generateRealLiveSystemStubs )
            genBase += ",org.nustaq.reallive.sys";
        if ( appconf.scan4Remote != null ) {
            for (int i = 0; i < appconf.scan4Remote.length; i++) {
                genBase+=","+appconf.scan4Remote[i];
            }
        }
        MB2JS.Gen(genBase, appRootDir+"/tmp/generated.js");
    }

    public IPromise<Consumer<FSTConfiguration>> $getRemotingConfigurator() {
        return new Promise<>( conf -> shortClassNameMapping.forEach( (k,v) -> conf.registerCrossPlatformClassMapping(k,v) ) );
    }
}

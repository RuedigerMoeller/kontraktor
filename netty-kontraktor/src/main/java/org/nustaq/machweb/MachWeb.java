package org.nustaq.machweb;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.http.netty.wsocket.ActorWSServer;
import org.nustaq.kontraktor.remoting.minbingen.MB2JS;
import org.nustaq.kson.Kson;
import org.nustaq.machweb.util.ScriptComponentLoader;

import java.io.File;
import java.util.*;

/**
 * Created by ruedi on 01.11.14.
 */
public abstract class MachWeb<SERVER extends Actor,SESSION extends MachWebSession> extends Actor<SERVER> {

    public static String FILEROOT = "./fileroot";

    protected Map<String, SESSION> sessions;
    protected long sessionIdCounter = 1;
    protected Scheduler clientScheduler; // set of threads processing client requests
    protected ServerConf conf;
    protected ScriptComponentLoader loader;

    @Local
    public void $init(Scheduler clientScheduler) {
        this.sessions = new HashMap<>();
        this.clientScheduler = clientScheduler;
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
    public Future<String> $authenticate(String user, String pwd) {
        Promise p = new Promise();
        isLoginValid(user, pwd).then(
          (result, e) -> {
            if (result != null ) {
                String sessionId = "" + sessionIdCounter++; // FIXME: use strings
                SESSION newSession = createSessionActor(sessionId, clientScheduler, result);
                newSession.$initFromServer(sessionId, (MachWeb) self());
                sessions.put(sessionId, newSession);
                p.receive(sessionId, null);
            } else {
                p.receive(null, "authentication failure");
            }
        });
        return p;
    }

    public Future<SESSION> $getSession(String id) {
        return new Promise<>(sessions.get(id));
    }

    @Local
    public Future $clientTerminated(SESSION session) {
        Promise p = new Promise();
        session.$getId().then((id, err) -> {
            sessions.remove(id);
            p.signal();
        });
        return p;
    }

    abstract protected Future<Object> isLoginValid(String user, String pwd);
    abstract protected SESSION createSessionActor(String sessionId, Scheduler clientScheduler, Object resultFromIsLoginValid);


    // grab port from command line args
    protected static void parseArgs(String[] arg, ServerConf conf) {
        int port = 0;
        if ( arg.length > 1 ) {
            System.out.println("Expect port as argument");
            System.exit(1);
        }
        if ( arg.length > 0 ) {
            try {
                port = Integer.parseInt(arg[0]);
                conf.port = port; // override conf
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Expect port as first argument");
                System.exit(1);
            }
        }
    }

    /**
     * startup server + map some files for development
     * @param arg
     * @throws Exception
     */
    public Future $main( String arg[] ) {
        try {
            // pacth conf from args
            conf = (ServerConf) new Kson().readObject(new File("conf.kson"), ServerConf.class.getName());
            parseArgs(arg,conf);
            generateRemoteStubs(conf);

            HashMap<String,String>
                shortClassNameMapping = (HashMap<String, String>) new Kson().readObject(new File("name-map.kson"),HashMap.class);

            ElasticScheduler scheduler = new ElasticScheduler(conf.clientThreads, conf.clientQSize);
            // install handler to automatically search and bundle jslibs + template snippets
            loader = new ScriptComponentLoader().setResourcePath(conf.componentPath);

            ((MachWeb) self()).$init(scheduler);

            // start websocket server (default path for ws traffic /websocket)
            ActorWSServer server = ActorWSServer.startAsRestWSServer(
                    conf.port,
                    self(),         // facade actor
                    new File(FILEROOT), // content root
                    scheduler,      // Scheduler determining per client q size + number of worker threads
                    new Coding(
                        SerializerType.MinBin,
                        fstConf -> shortClassNameMapping.forEach( (k,v) -> fstConf.registerCrossPlatformClassMapping(k,v) )
                    )
            );
            installVirtualFileMappers(server);


        } catch (Exception e) {
            e.printStackTrace();
            return new Promise<>(null, e);
        }
        return new Promise<>("void");
    }

    protected void installVirtualFileMappers(ActorWSServer server) {
        // e.g. src='lookup/dir/bootstrap.css will search for first dir/bootstrap.css on component path
        server.setFileMapper( (f) -> {
            String prefix = FILEROOT + "/lookup";
            if ( f.getPath().replace(File.separatorChar,'/').startsWith(prefix) ) {
                List<File> files = loader.lookupResource(f.getPath().substring(prefix.length() + 1), new HashSet<>(), new HashSet<>());
                if ( files.size() > 0 )
                    return files.iterator().next();
            }
            return f;
        });

        server.setVirtualfileMapper((f) -> {
            if (f.getName().equals("libs.js")) {
                return loader.mergeScripts(conf.components);
            } else if (f.getName().equals("templates.js")) {
                return loader.mergeTemplateSnippets(conf.components);
            }
            return null;
        });
    }

    protected void generateRemoteStubs(ServerConf appconf) throws Exception {
        String genBase = "org.nustaq.reallive.sys";
        if ( appconf.scan4Remote != null ) {
            for (int i = 0; i < appconf.scan4Remote.length; i++) {
                genBase+=","+appconf.scan4Remote[i];
            }
        }
        MB2JS.Gen(genBase, "generated.js");
    }


}
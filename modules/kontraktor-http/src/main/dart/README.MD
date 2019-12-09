# Long-Poll client for http/json published kontraktor actors

example Server
´´´java

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.apputil.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.webapp.javascript.clojure.ClojureJSPostProcessor;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.MapRecord;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ExampleServer extends Actor<ExampleServer> implements SessionHandlingMixin<ExampleServer>, RegistrationMixin<ExampleServer> {

    public static ExampleServerConfig cfg;

    // threads to dispatch session onto
    private Scheduler clientThreads[];
    private Random rand = new Random();
    private ExampleServerService service;
    private DataClient dclient;

    @Local
    public void init(String args[]) {
        clientThreads = new Scheduler[cfg.getNumSessionThreads()];
        IntStream.range(0,cfg.getNumSessionThreads())
            .forEach( i -> clientThreads[i] = new SimpleScheduler(10000, true /*Important!*/ ));
        service = ExampleServerService.start(args);
        service.setWebServer(self());
        dclient = service.getDClient();
    }

    @CallerSideMethod
    public DataClient getDClient() {
        return getActor().dclient;
    }

    public IPromise test( String arg0 ) {
        return resolve("hello "+arg0);
    }

    public IPromise test1( String arg0, Callback cb ) {
        delayed(1000, () -> cb.pipe(arg0+" ->1"));
        delayed(2000, () -> cb.pipe(arg0+" ->2"));
        delayed(3000, () -> cb.complete());
        return resolve("hello callback "+arg0);
    }

    ////////////////////// Session handling //////////////////////////////////////////////

    public IPromise login(String email, String pwd, Callback events ) {
        if ( "".equals(email.trim()) ) {
            return reject("empty email");
        }
        Promise p = new Promise();
        getDClient().tbl(UserTableName).get(email.toLowerCase()).then( (r,e) -> {
            if ( r != null ) {
                UserRecord user = new UserRecord(r);
                if ( pwd.equals(user.getPwd()) ) {
                    ExampleSession session = AsActor(
                        ExampleSession.class,
                        // randomly distribute session actors among clientThreads
                        clientThreads[rand.nextInt(clientThreads.length)]
                    );
                    session.init(user,self(),events);
                    p.resolve(new LoginData().session(session).user(user)); // == new Promise(session)
                } else {
                    p.reject("wrong user or password");
                }
            } else {
                p.reject("wrong user or password");
            }
        });
        return p;
    }


    ///////////////////// config and startup ////////////////////////////////////////////

    public static void main(String[] args) throws InterruptedException {

        if ( ! new File("./src/main/web/index.html").exists() ) {
            System.out.println("Please run with project working dir");
            System.exit(-1);
        }

        // separate&remove for distributed / clustered setup
        ExampleDataClusterStartup.main(new String[0]);

        ExampleServerConfig cfg = ExampleServerConfig.read("./run/etc/config.kson");
        ExampleServer.cfg = cfg;

        ExampleServer app = AsActor(ExampleServer.class);
        app.init(new String[] { "-sp", ""+5678, "-monitorport", "8082" } /*args*/);

        Mailer.DEBUG_MAIL = cfg.isDevMode();
        try {
            MailCfg mc = MailCfg.read("./run/etc/mailcfg.kson");
            Mailer.initSingleton( mc,cfg.publicUrl );
        } catch (Exception e) {
            e.printStackTrace();
            Mailer.initSingleton( new MailCfg(),cfg.publicUrl );
        }

        Class CLAZZES[] = {
            LoginData.class,
            SessionEvent.class,
        };

        Http4K.Build(cfg.getBindIp(), cfg.getBindPort())
            .fileRoot( "imgupload","./run/upload/image")
            .fileRoot( "img","./run/data/img")
            .httpAPI("/api", app) // could also be websocket based (see IntrinsicReactJSX github project)
                .coding(new Coding(SerializerType.JsonNoRef, CLAZZES))
                .setSessionTimeout(TimeUnit.MINUTES.toMillis(cfg.getSessionTimeoutMinutes() ))
                .buildHttpApi()
            .build();

    }

}

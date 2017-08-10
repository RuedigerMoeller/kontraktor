package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.babel.BabelOpts;
import org.nustaq.kontraktor.babel.BrowseriBabelify;
import org.nustaq.kontraktor.babel.JSXWithBabelTranspiler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * simplifies setup of a undertow-kontraktor webserver + some lib specific tools (e.g. react, babel).
 * Common options are read from BasicWebAppConfig, further customization can be done by overriding methods.
 */
public class UndertowWebServerMain {

    /**
     * util to startup babel/browserify daemon
     * @return true if successful
     */
    public boolean runNodify() {
        try {
            BrowseriBabelify.get();
        } catch (Exception ex) {
            Log.Warn(BasicWebAppActor.class,"babelserver not running .. try starting");
            boolean isWindows = System.getProperty("os.name","linux").toLowerCase().indexOf("windows") >= 0;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (isWindows) {
                    processBuilder.command("cmd.exe", "/c", "node "+ BasicWebAppActor.BABEL_SERVER_JS_PATH);
                } else {
                    String bash = BasicWebAppActor.BASH_EXEC;
                    if ( !new File(bash).exists() ) {
                        bash = "/bin/bash";
                    }
                    processBuilder.command(bash, "-c", "node "+ BasicWebAppActor.BABEL_SERVER_JS_PATH);
                }
                processBuilder.directory(new File(BasicWebAppActor.WEBAPP_DIR));
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

    public BasicWebAppActor reactMainHelper(boolean intrinsicJSX, Class<? extends BasicWebAppActor> myHttpAppClass, BasicWebAppConfig cfg) throws IOException {

        boolean DEV = cfg.getDev();

        if ( ! intrinsicJSX ) {
            // check/start node babelserver daemon (if this fails run babeldaemon manually, e.g. windoze
            checkBabelService();
        }

        // just setup stuff manually here. Its easy to buildResourcePath an application specific
        // config using e.g. json or kson.
        File root = new File(cfg.getClientRoot());

        checkWorkingDir(root);

        // create server actor
        BasicWebAppActor myHttpApp = setupAppActor(myHttpAppClass, cfg);

        setupUndertow4K_React(intrinsicJSX,cfg, myHttpApp);
        return myHttpApp;
    }

    protected void setupUndertow4K_React(boolean intrinsicJSX, BasicWebAppConfig cfg, BasicWebAppActor myHttpApp) {
        // these classes are encoded with Simple Name in JSon
        Class msgClasses[] = cfg.getMessageClasses();
        Http4K.Build(cfg.getHost(), cfg.getPort())
            .fileRoot(cfg.getStaticUrlPrefix(), cfg.getStaticFileRoot())
            .resourcePath("/")
                .elements(
                    cfg.getClientRoot(),
                    cfg.getClientRoot()+"/node_modules"
                )
                .allDev(cfg.dev)
                .transpile("jsx", createJSXTranspiler(intrinsicJSX,cfg))
                .handlerInterceptor( exchange -> {
                    // can be used to intercept (e.g. redirect or raw response) all requests coming in on this resourcepath
                    String requestPath = exchange.getRequestPath();
                    if ( requestPath == null || !requestPath.startsWith(getRawRequestUrlPrefix()) ) {
                        return false;
                    }
                    exchange.dispatch();
                    myHttpApp.handleDirectRequest(exchange);
                    return true;
                })
                .buildResourcePath()
            .httpAPI(getApiEndpointUrlPath(), myHttpApp)
                .coding(new Coding(SerializerType.JsonNoRef,msgClasses))
                .setSessionTimeout(cfg.getSessionTimeoutMS())
                .buildHttpApi()
            .build();
    }

    protected JSXWithBabelTranspiler createJSXTranspiler(boolean intrinsicJSX, BasicWebAppConfig cfg) {
        return new JSXWithBabelTranspiler().opts(new BabelOpts().debug(cfg.dev));
    }

    /**
     * note client must be changed if this is changed
     * @return
     */
    protected String getApiEndpointUrlPath() {
        return "/ep";
    }

    /**
     * requests to urls with the given prefix will be routed raw to "handleDirectRequest" of the WebAppActor. This can be used
     * to implement intelligent redirects, serving of generated html or to process confirmation links from opt-in emails.
     * @return
     */
    protected String getRawRequestUrlPrefix() {
        return "/direct/";
    }

    protected BasicWebAppActor setupAppActor(Class<? extends BasicWebAppActor> myHttpAppClass, BasicWebAppConfig cfg) {
        BasicWebAppActor myHttpApp = AsActor(myHttpAppClass);
        myHttpApp.init(cfg);
        return myHttpApp;
    }

    protected void checkWorkingDir(File root) {
        if ( ! new File(root,"index.html").exists() ) {
            System.out.println("Please run with working dir: '[..]examples/react");
            System.exit(2);
        }
    }

    protected void checkBabelService() {
        if ( !runNodify() ) {
            System.out.println("failed to connect / start babel");
            System.exit(1);
        }
    }

}

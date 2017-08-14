package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.webapp.babel.BabelOpts;
import org.nustaq.kontraktor.webapp.babel.BrowseriBabelify;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;
import org.nustaq.kontraktor.webapp.transpiler.JSXWithBabelTranspiler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.transpiler.TranspileException;
import org.nustaq.kontraktor.webapp.transpiler.TranspilerHook;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXGenerator;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXParser;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.util.List;
import java.util.Set;

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
                    getResourcePathElements(cfg)
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

    protected String[] getResourcePathElements(BasicWebAppConfig cfg) {
        return new String[]{
            cfg.getClientRoot(),
            cfg.getClientRoot() + "/../lib",
            cfg.getClientRoot() + "/../node_modules",
            cfg.getClientRoot() + "/../bower_components"
        };
    }

    protected TranspilerHook createJSXTranspiler(boolean intrinsicJSX, BasicWebAppConfig cfg) {
        if ( intrinsicJSX ) {
            return new JSXIntrinsicTranspiler(cfg.dev, !cfg.dev);
        } else {
            return new JSXWithBabelTranspiler().opts(new BabelOpts().debug(cfg.dev));
        }
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

    protected String generateImportEnd(String name, JSXGenerator.ParseResult result) {
        String s = "\n";
        String exportObject = "kimports." + constructLibName(name);
        s += "  "+exportObject+" = {};\n";
        for (int i = 0; i < result.getGlobals().size(); i++) {
            String gl = result.getGlobals().get(i);
            s+="  "+exportObject+"."+gl+" = "+gl+";\n";
        }
        return s+"});";
    }

    protected String generateImportPrologue(String name, JSXGenerator.ParseResult result) {
        String s = "window.klibmap = window.klibmap || {};\nwindow.kimports = window.kimports || {};\n";
        s += "(new function() {\n";
        List<JSXParser.ImportSpec> imports = result.getImports();
        for (int i = 0; i < imports.size(); i++) {
            JSXParser.ImportSpec spec = imports.get(i);
            String libname = constructLibName(spec.getFrom());
            String exportObject = "kimports." + libname;
            if ( spec.getAlias() != null ) {
                s+="  const "+spec.getAlias()+" = "+exportObject+"."+spec.getComponent()+";\n";
            }
            for (int j = 0; j < spec.getAliases().size(); j++) {
                String alias = spec.getAliases().get(j);
                s+="  const "+alias+" = _kresolve('"+libname+"', '"+spec.getComponents().get(j)+"');\n";
//                s+="  const "+alias+" = klibmap."+libname+"? klibmap."+libname+"()"+"."+spec.getComponents().get(j)
//                    +":"+exportObject+"."+spec.getComponents().get(j)+";\n";
            }
        }
        s += "\n";
        return s;
    }

    protected String constructLibName(String name) {
        name = JSXGenerator.camelCase(new File(name).getName());
        return name.replace(".jsx","").replace(".js","");
    }

}

package org.nustaq.kontraktor.undertow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpObjectSocket;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServer;
import org.nustaq.kontraktor.undertow.websockets.KUndertowWebSocketHandler;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A simple webserver created by wrapping the undertow webserver + some preconfiguration.
 *
 * - roothandler is a pathhandler having registered a static FileResourceHandler at "/".
 *
 *
 * by overriding various creatXX methods its possible to basically change everything.
 * Ofc its also easy to create your own Undertow stub and just hang in Kontrakor's provided HttpHandlers
 * as needed. So this is kind of a blueprint
 *
 */
public class Knode {

    protected Undertow server;
    protected PathHandler pathHandler; // is default handler
    protected Options options;

    public void start(Options options) {
        this.options = options;
        pathHandler = createPathHandler();
        server = createServer();
        server.start();
    }

    public Options getOptions() {
        return options;
    }

    public Undertow getServer() {
        return server;
    }

    public void stop() {
        getServer().stop();
    }

    /**
     * use this to add in handlers using addXX methods
     *
     * @return
     */
    public PathHandler getPathHandler() {
        return pathHandler;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //  factory/setup methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public Undertow createServer() {
        return Undertow.builder()
                   .setIoThreads( options.getIoThreads())
                   .setWorkerThreads(options.getWorkerThreads())
                   .addHttpListener(options.getPort(), options.getHost())
                   .setHandler(createRootHttpHandler()).build();
    }

    protected HttpHandler createRootHttpHandler() {
        return exchange -> pathHandler.handleRequest(exchange);
    }

    public void publishOnWebsocket( String prefixPath, SerializerType encoding, Actor actor, Consumer<FSTConfiguration> configurator ) {
        WebSocketActorServer webSocketActorServer
            = new WebSocketActorServer(
                new KUndertowHttpServerAdapter(getServer(),getPathHandler()),
                new Coding(encoding, configurator),
                actor
        );
        getPathHandler().addExactPath(prefixPath,
            Handlers.websocket(
                KUndertowWebSocketHandler.With(webSocketActorServer)
            )
        );
    }

    ConcurrentHashMap<String, RestActorServer> installedHttpServers = new ConcurrentHashMap<>();
    public void publishOnHttp( String prefixPath, String serviceName, Actor service ) {
        RestActorServer raServer = installedHttpServers.get(prefixPath);
        if ( raServer == null ) {
            KUndertowHttpServerAdapter sAdapt = new KUndertowHttpServerAdapter(
                getServer(),
                getPathHandler()
            );
            raServer = new RestActorServer();
            raServer.joinServer(prefixPath, sAdapt);
            installedHttpServers.put(prefixPath,raServer);
        }
        raServer.publish( serviceName, service);
    }

    protected PathHandler createPathHandler() {
        PathHandler pathHandler = new PathHandler();
        HttpHandler handler = createDefaultHandler();
        pathHandler.addPrefixPath(
            "/",
            handler
        );
        return pathHandler;
    }


    protected HttpHandler createDefaultHandler() {
        FileResourceManager fileResourceManager = createResourceManager();
        ResourceHandler resourceHandler = new ResourceHandler(fileResourceManager);
        // undertow already serves static files in a separate thread
        return resourceHandler;
    }

    protected FileResourceManager createResourceManager() {
        return new FileResourceManager(new File( "./webroot" ), 100);
    }

    public void mainStub(String a[]) {
        Options options = new Options();

        JCommander com = new JCommander();
        com.addObject(options);
        try {
            com.parse(a);
        } catch (Exception ex) {
            System.out.println("command line error: '"+ex.getMessage()+"'");
            options.help = true;
        }
        if ( options.help ) {
            com.usage();
            return;
        }
        start(options);
    }

    public static class Options {
        @Parameter( names = { "-h", "-host" }, description = "the hostname" )
        String host = "localhost";
        @Parameter( names = { "-p", "-port"}, description = "port to serve on" )
        int port = 8080;
        @Parameter( names = { "-?", "-help", "--help" }, description = "display help")
        boolean help;

        // just used to pass stuff to kontraktor
        public int workerThreads = 1;
        @Parameter( names = {"-iot","-iothreads"}, description = "undertow epoll threads. just used for polling io, not processing")
        public int ioThreads = 2;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public int getIoThreads() {
            return ioThreads;
        }
    }

    public static void main(String a[]) {
        try {
            Knode knode = new Knode();
            knode.mainStub(a);
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(-1);
        }
    }

}

package org.nustaq.kontraktor.remoting.http;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.http.builder.CFGDirRoot;
import org.nustaq.kontraktor.remoting.http.builder.CFGFourK;
import org.nustaq.kontraktor.remoting.http.builder.CFGResPath;
import org.nustaq.kontraktor.remoting.http.javascript.DynamicResourceManager;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.Pair;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 25/05/15.
 *
 * singleton to manage http server instances. Currently tied to Undertow however implicitely shields kontraktor
 * from getting too dependent on Undertow (which is an excellent piece of software, so no plans to migrate anytime soon)
 *
 */
public class Http4K {

    protected static Http4K instance;
    public static void set(Http4K http) {
        instance = http;
    }

    public static Http4K get() {
        synchronized (Http4K.class) {
            if ( instance == null ) {
                instance = new Http4K();
            }
            return instance;
        }
    }

    public static CFGFourK Build( String hostName, int port, SSLContext ctx ) {
        return get().builder(hostName,port,ctx);
    }

    public static CFGFourK Build( String hostName, int port) {
        return get().builder(hostName,port,null);
    }

    // a map of port=>server
    protected Map<Integer, Pair<PathHandler,Undertow>> serverMap = new HashMap<>();

    /**
     * creates or gets an undertow web server instance mapped by port.
     * hostname must be given in case a new server instance has to be instantiated
     *
     * @param port
     * @param hostName
     * @return
     */
    public synchronized Pair<PathHandler, Undertow> getServer(int port, String hostName) {
        Pair<PathHandler, Undertow> pair = serverMap.get(port);
        if (pair == null) {
            PathHandler pathHandler = new PathHandler();
            Undertow.Builder builder = Undertow.builder()
                                           .setIoThreads(2)
                                           .setWorkerThreads(2);
            Undertow server = customize(builder,pathHandler,port,hostName).build();
            server.start();
            pair = new Pair<>(pathHandler,server);
            serverMap.put(port,pair);
        }
        return pair;
    }

    public CFGFourK builder(String hostName, int port, SSLContext ctx) {
        return new CFGFourK(hostName,port,ctx);
    }

    public CFGFourK builder(String hostName, int port) {
        return new CFGFourK(hostName,port,null);
    }

    protected Undertow.Builder customize(Undertow.Builder builder, PathHandler rootPathHandler, int port, String hostName) {
        return builder
                .addHttpListener(port, hostName)
//                .addHttpsListener(8443,hostName,null)
                .setHandler(rootPathHandler);
    }

    /**
     * publishes given file root
     * @param hostName
     * @param urlPath - prefixPath (e.g. /myapp/resource)
     * @param port
     * @param root - directory to be published
     */
    public Http4K publishFileSystem( String hostName, String urlPath, int port, File root ) {
        if ( ! root.isDirectory() ) {
            throw new RuntimeException("root must be an existing direcory");
        }
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        server.car().addPrefixPath(urlPath, new ResourceHandler(new FileResourceManager(root,100)));
        return this;
    }

    public Http4K publishResourcePath( String hostName, String urlPath, int port, DynamicResourceManager man, boolean compress ) {
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        ResourceHandler handler = new ResourceHandler(man);
        if ( compress ) {
            HttpHandler httpHandler = new EncodingHandler.Builder().build(new HashMap<>()).wrap(handler);
            server.car().addPrefixPath( urlPath, httpHandler);
        } else {
            server.car().addPrefixPath( urlPath, handler);
        }
        return this;
    }

    /**
     * utility, just redirects to approriate connector
     *
     * Publishes an actor/service via websockets protocol with given encoding.
     * if this should be connectable from non-java code recommended coding is 'new Coding(SerializerType.JsonNoRefPretty)' (dev),
     * 'new Coding(SerializerType.JsonNoRef)' (production)
     *
     * SerializerType.FSTSer is the most effective for java to java communication.
     *
     */
    public IPromise<ActorServer> publish( WebSocketPublisher publisher ) {
        return publisher.publish();
    }

    /**
     * utility, just redirects to approriate connector.
     */
    public IPromise<ActorServer> publish( HttpPublisher publisher ) {
        return publisher.publish();
    }

}

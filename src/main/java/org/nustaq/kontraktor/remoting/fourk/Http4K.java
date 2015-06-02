package org.nustaq.kontraktor.remoting.fourk;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.http.UndertowHttpServerConnector;
import org.nustaq.kontraktor.remoting.websockets.UndertowWebsocketServerConnector;
import org.nustaq.kontraktor.util.Pair;

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
            Undertow server = Undertow.builder()
                    .setIoThreads(getIoThreads())
                    .setWorkerThreads(getWorkerThreads())
                    .addHttpListener( port, hostName)
                    .setHandler(pathHandler)
                    .build();
            server.start();
            pair = new Pair<>(pathHandler,server);
            serverMap.put(port,pair);
        }
        return pair;
    }

    protected int getIoThreads() {return 2;}
    protected int getWorkerThreads() {return 2;}

    /**
     * publishes given file root
     * @param hostName
     * @param urlPath - prefixPath (e.g. /myapp/resource)
     * @param port
     * @param root - directory to be published
     */
    public void publishFileSystem( String hostName, String urlPath, int port, File root ) {
        if ( ! root.isDirectory() ) {
            throw new RuntimeException("root must be an existing direcory");
        }
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        server.car().addPrefixPath(urlPath, new ResourceHandler(new FileResourceManager(root,100)));
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
     * @param act
     * @param hostName
     * @param urlPath
     * @param port
     * @param coding
     */
    public Promise<ActorServer> publishOnWebSocket( Actor act, String hostName, String urlPath, int port, Coding coding ) {
        return UndertowWebsocketServerConnector.Publish(act, hostName, urlPath, port, coding);
    }

    /**
     * utility, just redirects to approriate connector.
     *
     * Publishes an actor/service via http with given encoding.
     * if this should be connectable from non-java code recommended coding is 'new Coding(SerializerType.JsonNoRefPretty)' (dev),
     * 'new Coding(SerializerType.JsonNoRef)' (production)
     *
     * SerializerType.FSTSer is the most effective for java to java communication.
     *
     * @param act
     * @param hostName
     * @param urlPath
     * @param port
     * @param coding
     */
    public Promise<ActorServer> publishOnHttp( Actor act, String hostName, String urlPath, int port, Coding coding ) {
        return UndertowHttpServerConnector.Publish(act, hostName, urlPath, port, coding);
    }

}

package org.nustaq.kontraktor.remoting.fourk;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
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

    public Object getGlobalLock() {
        return Http4K.class;
    }

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
        server.getFirst().addPrefixPath(urlPath, new ResourceHandler(new FileResourceManager(root,100)));
    }

}

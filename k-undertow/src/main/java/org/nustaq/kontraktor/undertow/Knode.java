package org.nustaq.kontraktor.undertow;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.io.File;

/**
 * Created by moelrue on 3/27/15.
 */
public class Knode extends Actor<Knode> {

    Undertow server;
    PathHandler pathHandler;

    public Future $start() {
        pathHandler = createPathHandler();
        HttpHandler firsthandler = exchange -> {
            self().$handleRequest(exchange);
        };
        Undertow server =
            Undertow.builder()
                .setIoThreads(2)
                .setWorkerThreads(1)
                .addHttpListener(getPort(), getHost())
                .setHandler(firsthandler).build();
        server.start();
        return new Promise<>("");
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

    protected String getHost() {
        return "localhost";
    }

    protected int getPort() {
        return 8080;
    }



    public void $handleRequest(HttpServerExchange exchange) {
        try {
            pathHandler.handleRequest(exchange);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String a[]) {
        Actors.AsActor(Knode.class).$start().await();
        System.out.println("Started");
    }

}

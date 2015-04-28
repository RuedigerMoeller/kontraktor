package org.nustaq.kontraktor.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.http.RestProcessor;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.undertow.http.KRestProcessorAdapter;
import org.nustaq.kontraktor.undertow.websockets.KUndertowWebSocketHandler;

/**
 * Created by ruedi on 03.04.2015.
 */
public class KUndertowHttpServerAdapter implements NioHttpServer {

    PathHandler pathHandler;
    Undertow tow;

    public KUndertowHttpServerAdapter(Undertow tow,PathHandler pathHandler) {
        this.tow = tow;
        this.pathHandler = pathHandler;
    }

    @Override
    public void addHandler(String path, RestProcessor restProcessor) {
        pathHandler.addPrefixPath( path, new KRestProcessorAdapter(restProcessor) );
    }

    @Override
    public void addHandler(String path, WebSocketActorServerAdapter webSocketServer) {
        KUndertowWebSocketHandler.WithResult wr = KUndertowWebSocketHandler.With(webSocketServer);
        pathHandler.addPrefixPath(path, wr.handler );
    }

    @Override
    public void removeHandler(String path) {
        pathHandler.removePrefixPath(path);
    }

    @Override
    public void startServer() {
        tow.start();
    }

    @Override
    public void stopServer() {
        tow.stop();
    }

    @Override
    public Actor getServingActor() {
        return null;
    }
}

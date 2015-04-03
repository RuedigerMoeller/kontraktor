package org.nustaq.kontraktor.undertow.websockets;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.http.RestProcessor;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServer;

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
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addHandler(String path, WebSocketActorServer webSocketServer) {
        WebSocketConnectionCallback handler = KUndertowWebSocketHandler.With(webSocketServer);
        pathHandler.addPrefixPath(path, new KUndertowWebSocketHandler(webSocketServer,handler));
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

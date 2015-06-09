package org.nustaq.kontraktor.remoting.http;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.builder.CFGFourK;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 04/06/15.
 *
 * Builder helper to publish an Actor via Http
 */
public class HttpPublisher implements ActorPublisher, Cloneable {

    CFGFourK cfg; // for builder

    String hostName;
    String urlPath;
    int port;
    Coding coding = new Coding(SerializerType.FSTSer);
    long sessionTimeout = TimeUnit.MINUTES.toMillis(30);
    Actor facade;

    public HttpPublisher() {}

    public HttpPublisher(Actor actor, String hostName, String urlPath, int port) {
        this.hostName = hostName;
        this.urlPath = urlPath;
        this.port = port;
        this.facade = actor;
    }

    public HttpPublisher(CFGFourK cfgFourK, Actor facade, String hostName, String urlPath, int port) {
        this(facade,hostName,urlPath,port);
        this.cfg = cfgFourK;
    }

    /**
     * usable in context of Http4k builder
     */
    public CFGFourK build() {
        return cfg;
    }

    /**
     * enables sharing of common settings if publishing also as websocket service
     * @return
     */
    public WebSocketPublisher toWS() {
        return new WebSocketPublisher()
            .coding(coding)
            .facade(facade)
            .hostName(hostName)
            .port(port)
            .urlPath(urlPath);
    }

    @Override
    public IPromise<ActorServer> publish() {
        ActorServer actorServer;
        try {
            facade.setThrowExWhenBlocked(true);
            Pair<PathHandler, Undertow> serverPair = Http4K.get().getServer(port, hostName);
            UndertowHttpServerConnector con = new UndertowHttpServerConnector(facade);
            con.setSessionTimeout(sessionTimeout);
            actorServer = new ActorServer( con, facade, coding == null ? new Coding(SerializerType.FSTSer) : coding );
            con.setActorServer(actorServer);
            actorServer.start();
            serverPair.getFirst().addPrefixPath(urlPath, con);
        } catch (Exception e) {
            Log.Warn(null, e);
            return new Promise<>(null,e);
        }
        return new Promise<>(actorServer);
    }

    public HttpPublisher hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public HttpPublisher urlPath(String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public HttpPublisher port(int port) {
        this.port = port;
        return this;
    }

    public HttpPublisher coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public HttpPublisher serType( SerializerType tp ) {
        return coding( new Coding( tp ) );
    }

    public HttpPublisher facade(Actor facade) {
        this.facade = facade;
        return this;
    }

    public HttpPublisher setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    @Override
    protected HttpPublisher clone() throws CloneNotSupportedException {
        HttpPublisher clone = (HttpPublisher) super.clone();
        return clone;
    }

    public String getHostName() {
        return hostName;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public int getPort() {
        return port;
    }

    public Coding getCoding() {
        return coding;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public Actor getFacade() {
        return facade;
    }
}

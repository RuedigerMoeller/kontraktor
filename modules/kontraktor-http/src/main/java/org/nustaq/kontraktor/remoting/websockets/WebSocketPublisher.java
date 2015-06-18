package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.builder.CFGFourK;

/**
 * Created by ruedi on 04/06/15.
 */
public class WebSocketPublisher implements ActorPublisher {

    CFGFourK cfg; // used in cfgbuilder

    String hostName;
    String urlPath;
    int port;
    Coding coding = new Coding(SerializerType.FSTSer);
    Actor facade;

    public WebSocketPublisher() {}

    public WebSocketPublisher(Actor facade, String host, String path, int port) {
        this.hostName = host;
        this.urlPath = path;
        this.port = port;
        this.facade = facade;
    }

    public WebSocketPublisher(CFGFourK cfgFourK, Actor facade, String hostName, String urlPath, int port) {
        this(facade,hostName,urlPath,port);
        this.cfg = cfgFourK;
    }

    public CFGFourK build() {
        return cfg;
    }

    @Override
    public IPromise<ActorServer> publish() {
        Promise finished = new Promise();
        try {
            ActorServer publisher = new ActorServer(new UndertowWebsocketServerConnector(urlPath,port,hostName), facade, coding);
            facade.execute(() -> {
                try {
                    publisher.start();
                    finished.resolve(publisher);
                } catch (Exception e) {
                    finished.reject(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise(null,e);
        }
        return finished;
    }

    public WebSocketPublisher hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public WebSocketPublisher urlPath(String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public WebSocketPublisher port(int port) {
        this.port = port;
        return this;
    }

    public WebSocketPublisher coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public WebSocketPublisher serType( SerializerType tp ) {
        return coding( new Coding( tp ) );
    }

    public WebSocketPublisher facade(Actor facade) {
        this.facade = facade;
        return this;
    }


}

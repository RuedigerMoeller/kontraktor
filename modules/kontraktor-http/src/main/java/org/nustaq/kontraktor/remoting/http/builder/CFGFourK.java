package org.nustaq.kontraktor.remoting.http.builder;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import org.nustaq.kontraktor.remoting.http.javascript.DynamicResourceManager;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 09.06.2015.
 */
public class CFGFourK {

    String hostName;
    int port;
    SSLContext context;

    List items = new ArrayList<>();

    public CFGFourK(String hostName, int port, SSLContext context) {
        this.hostName = hostName;
        this.port = port;
        this.context = context;
    }

    public CFGFourK fileRoot(String urlPath, String dir) {
        CFGDirRoot rt = new CFGDirRoot(urlPath,dir);
        items.add(rt);
        return this;
    }

    public CFGFourK fileRoot(String urlPath, File dir) {
        CFGDirRoot rt = new CFGDirRoot(urlPath,dir.getAbsolutePath());
        items.add(rt);
        return this;
    }

    public CFGResPath resourcePath(String urlPath) {
        CFGResPath rt = new CFGResPath(this,urlPath);
        items.add(rt);
        return rt;
    }

    public WebSocketPublisher websocket( String urlPath, Actor facade ) {
        WebSocketPublisher wp = new WebSocketPublisher(this,facade, hostName, urlPath, port);
        items.add(wp);
        return wp;
    }

    public HttpPublisher httpAPI(String urlPath, Actor facade) {
        HttpPublisher hp = new HttpPublisher( this, facade, hostName, urlPath, port);
        items.add(hp);
        return hp;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public SSLContext getContext() {
        return context;
    }

    public List getItems() {
        return items;
    }

    public void build() {
        Http4K http4K = Http4K.get();
        http4K.getServer(getPort(), getHostName());//fixme https
        getItems().forEach(item -> {
            if (item instanceof HttpPublisher) {
                http4K.publish((HttpPublisher) item);
            } else if (item instanceof WebSocketPublisher) {
                http4K.publish((WebSocketPublisher) item);
            } else if (item instanceof CFGDirRoot) {
                CFGDirRoot dr = (CFGDirRoot) item;
                http4K.publishFileSystem(getHostName(), dr.getUrlPath(), getPort(), new File(dr.getDir()));
            } else if (item instanceof CFGResPath) {
                CFGResPath dr = (CFGResPath) item;
                DynamicResourceManager drm = new DynamicResourceManager(dr.isDevMode(), dr.getUrlPath(), dr.getRootComponent(), dr.getResourcePath());
                http4K.publishResourcePath(getHostName(), dr.getUrlPath(), getPort(), drm);
            } else {
                System.out.println("unexpected item " + item);
            }
        });
    }
}

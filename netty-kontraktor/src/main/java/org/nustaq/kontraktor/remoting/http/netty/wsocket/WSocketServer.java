package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;

import java.io.File;

/**
 * Created by ruedi on 29.08.2014.
 */
public class WSocketServer extends ActorWSServer {

    Actor facade;

    public WSocketServer(File contentRoot) {
        super(contentRoot);
    }

    @Override
    protected Class getClientActorClazz() {
        return WSocketConnection.class;
    }
}

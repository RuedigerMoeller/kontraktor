package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;

import java.io.File;

/**
 * Created by ruedi on 29.08.2014.
 */
public class WSocketActorServer extends ActorWSServer {

    Actor facade;

    public WSocketActorServer(Actor facade, File contentRoot) {
        super(contentRoot);
        this.facade = facade;
    }

    @Override
    protected Class getClientActorClazz() {
        return WSocketServerSession.class;
    }

}

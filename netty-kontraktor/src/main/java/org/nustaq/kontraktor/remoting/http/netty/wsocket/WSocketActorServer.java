package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
import org.nustaq.webserver.ClientSession;

import java.io.File;

/**
 * Created by ruedi on 29.08.2014.
 *
 * An actor based websocket server. For each client connecting, a WSocketServerSession is created.
 * A remote client talks to the facade actor.
 *
 */
public class WSocketActorServer extends ActorWSServer {

    Actor facade;

    public WSocketActorServer(Actor facade, File contentRoot) {
	    this(facade, contentRoot, Coding.FSTSer);
    }

    public WSocketActorServer(Actor facade, File contentRoot, Coding coding ) {
        super(contentRoot, coding);
        this.facade = facade;
    }

	@Override
    protected Class getClientActorClazz() {
        return WSocketServerSession.class;
    }

}

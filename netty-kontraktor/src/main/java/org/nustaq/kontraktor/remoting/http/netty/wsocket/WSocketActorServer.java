package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;

import java.io.File;

/**
 * Created by ruedi on 29.08.2014.
 *
 * Subclass of actorwebsocket server. Implements full abstraction on actor remoting (websocket methods
 * not visible to user app)
 */
public class WSocketActorServer extends ActorWSServer {

    protected Actor facade; // the one and only facade actor (=application class)

    public WSocketActorServer(Actor facade, File contentRoot) {
	    this(facade, contentRoot, RemoteRefRegistry.Coding.FSTSer);
    }

    public WSocketActorServer(Actor facade, File contentRoot, RemoteRefRegistry.Coding coding ) {
        super(contentRoot, coding);
        this.facade = facade;
    }

	@Override
    protected Class getClientActorClazz() {
        return WSocketServerSession.class;
    }

}

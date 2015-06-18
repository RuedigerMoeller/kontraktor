package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;

/**
 * Created by ruedi on 18/06/15.
 */
public class TCPPublisher extends TCPNIOPublisher {

    public TCPPublisher() {
        super();
    }

    public TCPPublisher(Actor facade, int port) {
        super(facade, port);
    }

    @Override
    public IPromise<ActorServer> publish() {
        return TCPServerConnector.Publish(facade,port,coding);
    }
}

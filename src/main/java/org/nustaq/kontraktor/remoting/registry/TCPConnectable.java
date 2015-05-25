package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;

/**
 * Created by ruedi on 19/05/15.
 */
public class TCPConnectable implements ConnectableActor {

    String host;
    int port;
    Class actorClz;

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return (IPromise<T>) TCPClientConnector.Connect(actorClz, host, port, disconnectCallback );
    }

}

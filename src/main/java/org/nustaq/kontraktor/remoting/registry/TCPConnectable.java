package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;

/**
 * Created by ruedi on 19/05/15.
 *
 * Describes a connectable remote actor
 *
 */
public class TCPConnectable implements ConnectableActor {

    String host;
    int port;
    Class actorClz;
    Coding coding;

    public TCPConnectable(String host, int port, Class actorClz ) {
        this(host,port,actorClz,null);
    }

    /**
     *
     * @param host - ip/host e.g. "192.168.4.5"
     * @param port - port
     * @param actorClz - actor clazz to connect to
     * @param c - coding defaults to FSTSer if null
     */
    public TCPConnectable(String host, int port, Class actorClz, Coding c ) {
        this.host = host;
        this.port = port;
        this.actorClz = actorClz;
        this.coding = c;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return (IPromise<T>) TCPClientConnector.Connect(actorClz, host, port, disconnectCallback, coding );
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Class getActorClz() {
        return actorClz;
    }

}

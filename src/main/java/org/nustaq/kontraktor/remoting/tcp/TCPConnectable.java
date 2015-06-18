package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
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
    Coding coding = new Coding(SerializerType.FSTSer);

    public TCPConnectable() {
    }

    /**
     *
     * @param host - ip/host e.g. "192.168.4.5"
     * @param port - port
     * @param actorClz - actor clazz to connect to
     */
    public TCPConnectable(Class actorClz, String host, int port) {
        this.host = host;
        this.port = port;
        this.actorClz = actorClz;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        Promise result = new Promise();
        Runnable connect = () -> {
            TCPClientConnector client = new TCPClientConnector(port,host,disconnectCallback);
            ActorClient connector = new ActorClient(client,actorClz,coding);
            connector.connect().then(result);
        };
        if ( ! Actor.inside() ) {
            TCPClientConnector.get().execute(() -> Thread.currentThread().setName("singleton remote client actor polling"));
            TCPClientConnector.get().execute(connect);
        }
        else
            connect.run();
        return result;
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

    public TCPConnectable host(String host) {
        this.host = host;
        return this;
    }

    public TCPConnectable port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public TCPConnectable actorClass(Class actorClz) {
        this.actorClz = actorClz;
        return this;
    }

    public TCPConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public TCPConnectable serType(SerializerType sertype) {
        this.coding = new Coding(sertype);
        return this;
    }

}

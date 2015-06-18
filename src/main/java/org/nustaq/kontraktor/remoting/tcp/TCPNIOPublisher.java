package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

/**
 * Created by ruedi on 18/06/15.
 */
public class TCPNIOPublisher implements ActorPublisher {

    Actor facade;
    int port = 6543;
    Coding coding = new Coding( SerializerType.FSTSer );

    public TCPNIOPublisher() {
    }

    public TCPNIOPublisher(Actor facade, int port) {
        this.facade = facade;
        this.port = port;
    }

    @Override
    public IPromise<ActorServer> publish() {
        return NIOServerConnector.Publish(facade,port,coding);
    }

    public TCPNIOPublisher serType( SerializerType type ) {
        coding = new Coding(type);
        return this;
    }

    public TCPNIOPublisher facade(final Actor facade) {
        this.facade = facade;
        return this;
    }

    public TCPNIOPublisher port(final int port) {
        this.port = port;
        return this;
    }

    public TCPNIOPublisher coding(final Coding coding) {
        this.coding = coding;
        return this;
    }

    public Actor getFacade() {
        return facade;
    }

    public int getPort() {
        return port;
    }

    public Coding getCoding() {
        return coding;
    }
}

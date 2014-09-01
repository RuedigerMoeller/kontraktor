package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;

/**
 * Created by ruedi on 29.08.2014.
 *
 * a single client connection of websocket server
 */
public class WSocketServerSession<T extends WSocketServerSession> extends ActorWSClientSession<T> {

    private Actor facade;
    RemoteRefRegistry registry = new RemoteRefRegistry();
    MyWSObjectSoocket socket;
    int polldelay = 10;

    @Override
    public void $init(ActorWSServer server, int sessionId) {
        super.$init(server, sessionId);
        this.facade = getServer().facade;
        socket = new MyWSObjectSoocket(registry.getConf());
        registry.publishActor(facade);
        $poll();
    }

    @Override
    public void $onBinaryMessage(byte[] buffer) {
        try {
            socket.nextRead = buffer;
            registry.currentObjectSocket.set(socket);
            registry.singleReceive(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void $poll() {
        boolean idle =false;
        try {
            registry.currentObjectSocket.set(socket); // required manually as tcp needs to set this once not with eah iter as WSocket
            idle = !registry.singleSendLoop(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( ! isStopped() ) {
            if ( !idle ) {
                self().$poll(); // fixme, mor high load batching may be appropriate her (do not enqueue, directly loop)
            }
            delayed( polldelay, () -> $poll() );
        }
    }

    WSocketActorServer getServer() {
        return (WSocketActorServer) server;
    }

    class MyWSObjectSoocket extends WSAbstractObjectSocket {

        MyWSObjectSoocket(FSTConfiguration conf) {
            super(conf);
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            sendBinaryMessage(conf.asByteArray((Serializable) toWrite));
        }

    }
}

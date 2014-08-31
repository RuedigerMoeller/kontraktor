package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;

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
        socket = new MyWSObjectSoocket();
        $poll();
    }

    @Override
    public void $onBinaryMessage(byte[] buffer) {
        try {
            socket.nextRead = buffer;
            registry.singleReceive(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void $poll() {
        boolean idle =false;
        try {
            idle = !registry.singleSendLoop(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( ! isStopped() ) {
            if ( !idle ) {
                self().$poll();
            }
            delayed( polldelay, () -> $poll() );
        }
    }

    WSocketActorServer getServer() {
        return (WSocketActorServer) server;
    }

    class MyWSObjectSoocket extends WSAbstractObjectSocket {

        @Override
        public void writeObject(Object toWrite) throws Exception {
            sendBinaryMessage(conf.asByteArray((Serializable) toWrite));
        }

    }
}

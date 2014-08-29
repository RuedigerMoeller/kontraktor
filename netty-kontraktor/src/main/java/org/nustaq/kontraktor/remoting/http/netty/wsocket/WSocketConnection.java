package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.webserver.ClientSession;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by ruedi on 29.08.2014.
 */
public class WSocketConnection<T extends WSocketConnection> extends ActorWSClientSession<T> {

    private Actor facade;
    RemoteRefRegistry registry = new RemoteRefRegistry();
    WebObjectSocket socket;
    int polldelay = 10;

    @Override
    public void $init(ActorWSServer server, int sessionId) {
        super.$init(server, sessionId);
        this.facade = getServer().facade;
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

    WSocketServer getServer() {
        return (WSocketServer) server;
    }

    class WebObjectSocket implements ObjectSocket {

        FSTConfiguration conf;

        byte nextRead[]; // fake as not polled

        @Override
        public Object readObject() throws Exception {
            if (nextRead == null)
                return null;
            final byte[] tmp = nextRead;
            nextRead = null;
            return conf.asObject(tmp);
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            sendBinaryMessage(conf.asByteArray((Serializable) toWrite));
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void setLastError(Exception ex) {

        }

        @Override
        public void close() throws IOException {

        }
    }
}

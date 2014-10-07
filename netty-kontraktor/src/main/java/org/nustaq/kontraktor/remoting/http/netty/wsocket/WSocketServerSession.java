package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSClientSession;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MBPrinter;

/**
 * Created by ruedi on 29.08.2014.
 *
 * a single client connection of websocket server
 */
public class WSocketServerSession<T extends WSocketServerSession> extends ActorWSClientSession<T> {

    public static int PING_INTERVAL_MILLIS = 10000;
    protected Actor facade;
    RemoteRefRegistry registry;
    MyWSObjectSocket socket;
    int polldelay = 10;

    @Override
    public void $init(ActorWSServer server, int sessionId, ActorWSServer.Coding coding) {
        super.$init(server, sessionId, coding);
        registry = new RemoteRefRegistry(coding) {
	        @Override
	        public Actor getFacadeProxy() {
	            return facade;
	        }
        };
        this.facade = getServer().facade;
        socket = new MyWSObjectSocket(registry.getConf());
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

    @Override
    public void $onTextMessage(String text) {
        super.$onTextMessage(text);
    }

    @Override
    public void $onOpen(ChannelHandlerContext ctx) {
        super.$onOpen(ctx);
        self().$runPing(PING_INTERVAL_MILLIS);
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
                self().$poll(); // fixme, more batching may be appropriate here (do not enqueue, directly loop)
            }
            delayed( polldelay, () -> $poll() );
        }
    }

    protected WSocketActorServer getServer() {
        return (WSocketActorServer) server;
    }

    @Override
    public void $onClose() {
        super.$onClose();
        registry.cleanUp();
    }

    class MyWSObjectSocket extends WSAbstractObjectSocket {

        MyWSObjectSocket(FSTConfiguration conf) {
            super(conf);
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            final byte[] b = conf.asByteArray(toWrite);
            sendBinaryMessage(b);
        }

    }
}

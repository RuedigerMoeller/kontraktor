package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.webserver.ClientSession;

/**
 * Created by ruedi on 28.08.14.
 *
 * Base session actor for websockets, receives raw websocket messages
 *
 */
public class ActorWSClientSession<T extends ActorWSClientSession> extends Actor<T> implements ClientSession {

    public static int NUM_MISSING_PONGS_FOR_TIMEOUT = 3;
    protected ChannelHandlerContext context;
    protected ActorWSServer server;
    protected int sessionId;
    volatile protected long lastPong = System.currentTimeMillis();

    public void $init(ActorWSServer server, int sessionId, Coding coding) {
        this.server = server;
        this.sessionId = sessionId;
    }

    /**
     * starts pinging client periodically
     * @param millis
     */
    public void $runPing(long millis) {
        if ( System.currentTimeMillis() - lastPong >= NUM_MISSING_PONGS_FOR_TIMEOUT * millis ) {
            server.removeSession(context);
            self().$onClose();
            self().$stop();
        }
        if ( ! isStopped() ) {
            server.sendWSPingMessage(context);
            delayed(millis, () -> self().$runPing(millis));
        }
    }

    protected void sendBinaryMessage(byte[] b) {
        server.sendWSBinaryMessage(context, b);
    }

    protected void sendTextMessage(String s) {
        server.sendWSTextMessage(context, s);
    }

    public void $onBinaryMessage(byte[] buffer) {

    }

    public void $onOpen(ChannelHandlerContext ctx) {
        this.context = ctx;
    }

    public void $onClose() {
        Log.Info(this, "on close on ws session");
    }

    public void $onTextMessage(String text) {
        if ( "KTR_PING".equals(text) ) { // some browsers do no send pings by themselfs
            $pong();
        }
    }

    public void $pong() {
        lastPong = System.currentTimeMillis();
    }
}

package org.nustaq.kontraktor.remoting.http.netty.util;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.Actor;
import org.nustaq.webserver.ClientSession;

/**
 * Created by ruedi on 28.08.14.
 */
public class ActorWSClientSession<T extends ActorWSClientSession> extends Actor<T> implements ClientSession {

    protected ChannelHandlerContext context;
    protected ActorWSServer server;
    protected int sessionId;

    public void $init(ActorWSServer server, int sessionId) {
        this.server = server;
        this.sessionId = sessionId;
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

    }

    public void $onTextMessage(String text) {

    }
}

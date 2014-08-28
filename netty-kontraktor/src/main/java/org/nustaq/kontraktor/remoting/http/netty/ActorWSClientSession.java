package org.nustaq.kontraktor.remoting.http.netty;

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


    public void $onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {

    }

    public void $onOpen(ChannelHandlerContext ctx) {

    }

    public void $onClose(ChannelHandlerContext ctx) {

    }

    public void $onTextMessage(ChannelHandlerContext ctx, String text) {

    }
}

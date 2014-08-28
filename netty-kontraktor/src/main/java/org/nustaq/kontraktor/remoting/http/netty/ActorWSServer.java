package org.nustaq.kontraktor.remoting.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 28.08.14.
 */
public abstract class ActorWSServer extends WebSocketHttpServer {

    // don't buffer too much.
    public static int CLIENTQ_SIZE = 1000;
    public static int MAX_THREADS = 1;

    Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);

    public ActorWSServer(File contentRoot) {
        super(contentRoot);
    }

    @Override
    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onBinaryMessage without session");
        } else {
            session.$onBinaryMessage(ctx, buffer);
        }
    }

    @Override
    public void onHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, NettyWSHttpServer.HttpResponseSender sender) {
        super.onHttpRequest(ctx, req, sender);
    }

    @Override
    public void onOpen(ChannelHandlerContext ctx) {
        super.onOpen(ctx);
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onOpen without session");
        } else {
            session.$onOpen(ctx);
        }
    }

    @Override
    public void onClose(ChannelHandlerContext ctx) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onClose without session");
        } else {
            session.$onClose(ctx);
        }
    }

    @Override
    public void onTextMessage(ChannelHandlerContext ctx, String text) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onTextMessage without session");
        } else {
            session.$onTextMessage(ctx, text);
        }
    }

    @Override
    protected ActorWSClientSession getSession(ChannelHandlerContext ctx) {
        return (ActorWSClientSession) super.getSession(ctx);
    }

    protected AtomicInteger sessionid = new AtomicInteger(1);

    @Override
    protected ClientSession createNewSession() {
        ActorWSClientSession session = Actors.AsActor((Class<? extends Actor>) getClientActorClazz(), clientScheduler);
        session.$init(this,sessionid.incrementAndGet());
        return session;
    }

    abstract protected Class getClientActorClazz();

}

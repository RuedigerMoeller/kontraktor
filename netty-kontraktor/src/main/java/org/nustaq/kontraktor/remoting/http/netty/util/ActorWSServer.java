package org.nustaq.kontraktor.remoting.http.netty.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MinBin;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 28.08.14.
 */
public abstract class ActorWSServer extends WebSocketHttpServer {

	public enum Coding {
		FSTSer,
		MinBin
	}
    // don't buffer too much.
    public static int CLIENTQ_SIZE = 1000;
    public static int MAX_THREADS = 1;

    protected Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);
	protected volatile Coding coding = Coding.FSTSer;


    public ActorWSServer(File contentRoot, Coding coding) {
        super(contentRoot);
	    this.coding = coding;
    }

    @Override
    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onBinaryMessage without session");
        } else {
            session.$onBinaryMessage(buffer);
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
            ctx.channel().close();
        } else {
            if ( ! session.isStopped() )
                session.$onClose();
            session.$stop();
            session.$close();
        }
    }

    @Override
    public void onTextMessage(ChannelHandlerContext ctx, String text) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onTextMessage without session");
        } else {
            session.$onTextMessage(text);
        }
    }

    @Override
    public void onPong(ChannelHandlerContext ctx) {
        ActorWSClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("pong without session");
        } else {
            session.$pong();
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
        session.$init(this,sessionid.incrementAndGet(), coding);
        return session;
    }

    abstract protected Class getClientActorClazz();

}

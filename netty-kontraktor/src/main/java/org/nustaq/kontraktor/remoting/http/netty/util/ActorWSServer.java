package org.nustaq.kontraktor.remoting.http.netty.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.netty.service.HttpRemotingServer;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 28.08.14.
 */
public abstract class ActorWSServer extends HttpRemotingServer {

    // don't buffer too much messages (memory issues with many clients)
    public static int CLIENTQ_SIZE = 1000;
    public static int MAX_THREADS = 1;

    protected Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);
	protected volatile RemoteRefRegistry.Coding coding = RemoteRefRegistry.Coding.FSTSer;
    protected AtomicInteger sessionid = new AtomicInteger(1);

    /**
     *
     * @param contentRoot - root to serve files from
     * @param coding - encoding used for websocket traffic
     */
    public ActorWSServer(File contentRoot, RemoteRefRegistry.Coding coding) {
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

    //
    // websocket stuff, http handled by superclass
    //

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

    @Override
    protected ClientSession createNewSession() {
        ActorWSClientSession session = Actors.AsActor((Class<? extends Actor>) getClientActorClazz(), clientScheduler);
        session.$init(this,sessionid.incrementAndGet(), coding);
        return session;
    }

    abstract protected Class getClientActorClazz();

}

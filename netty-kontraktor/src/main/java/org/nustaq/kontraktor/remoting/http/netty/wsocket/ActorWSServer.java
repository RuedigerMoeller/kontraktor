package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.http.ServeFromCPProcessor;
import org.nustaq.kontraktor.remoting.http.netty.service.HttpRemotingServer;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * implements mechanics of transparent actor remoting via websockets based on the websocket api
 * inherited.
 *
 * FIXME: inherited Per-Client session actor is superfluous and adds latency per actor caused by redundant message enqueuing
 * FIXME: currently flow is httpserver =enqueue=> actor server =enq=> connection session =enq=> AppServerActor/AppSessionActor
 */
public class ActorWSServer extends HttpRemotingServer {

    // don't buffer too much messages (memory issues with many clients)
    public static int DEFAULT_CLIENTQ_SIZE = 1000;
    public static int DEFAULT_MAX_THREADS = 1;

    /**
     * startup given actor as a RestService AND as a Websocket Service.
     * @param port
     * @param actServer
     * @param contentRoot
     * @param clientScheduler
     * @return
     * @throws Exception
     */
    public static ActorWSServer startAsRestWSServer(int port, Actor actServer, File contentRoot, Scheduler clientScheduler) throws Exception {
        ActorWSServer server = new ActorWSServer( actServer, contentRoot, RemoteRefRegistry.Coding.MinBin, clientScheduler );

        // setup a restactor server under /rest
        RestActorServer restServer = new RestActorServer();
        restServer.publish("rest",actServer);
        restServer.joinServer(server);
        server.$addHttpProcessor(new ServeFromCPProcessor()); // FIXME: order should be file => classpath;

        new Thread( () -> {
            try {
                new NettyWSHttpServer(port, server).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Netty Main").start();
        return server;
    }

    protected Scheduler clientScheduler;
	protected volatile RemoteRefRegistry.Coding coding = RemoteRefRegistry.Coding.FSTSer;
    protected AtomicInteger sessionid = new AtomicInteger(1);

    protected Actor facade; // the one and only facade actor (=application class)

    /**
     *
     * @param contentRoot - root to serve files from
     * @param coding - encoding used for websocket traffic
     */
    public ActorWSServer( Actor facade, File contentRoot, RemoteRefRegistry.Coding coding, Scheduler clientScheduler) {
        super(contentRoot);
        this.clientScheduler = clientScheduler;
	    this.coding = coding;
        this.facade = facade;
    }

    public ActorWSServer( Actor facade, File contentRoot, RemoteRefRegistry.Coding coding) {
        this( facade, contentRoot,coding,new ElasticScheduler(DEFAULT_MAX_THREADS, DEFAULT_CLIENTQ_SIZE) );
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

    protected Class getClientActorClazz() {
        return WSocketServerSession.class;
    }

}

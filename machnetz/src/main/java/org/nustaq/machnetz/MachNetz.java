package org.nustaq.machnetz;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.util.concurrent.atomic.*;

/**
 * Created by ruedi on 25.05.14.
 */
public class MachNetz extends WebSocketHttpServer {

    // FIXME: need exception mode for blocking clients

    // don't buffer too much.
    public static int CLIENTQ_SIZE = 1000;
    public static int MAX_THREADS = 8;

    Scheduler clientScheduler = new ElasticScheduler(MAX_THREADS, CLIENTQ_SIZE);

    public MachNetz(File contentRoot) {
        super(contentRoot);
    }


    @Override
    public void onOpen(ChannelHandlerContext ctx) {
        super.onOpen(ctx);
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onOpen without session");
        } else {
            session.$onOpen(ctx);
        }
    }

    @Override
    public void onClose(ChannelHandlerContext ctx) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onClose without session");
        } else {
            session.$onClose(ctx);
        }
    }

    @Override
    public void onTextMessage(ChannelHandlerContext ctx, String text) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onTextMessage without session");
        } else {
            session.$onTextMessage(ctx, text);
        }
    }

    @Override
    public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
        MNClientSession session = getSession(ctx);
        if ( session == null ) {
            logger.warning("onBinaryMessage without session");
        } else {
            session.$onBinaryMessage(ctx, buffer);
        }
    }

    @Override
    protected MNClientSession getSession(ChannelHandlerContext ctx) {
        return (MNClientSession) super.getSession(ctx);
    }

    AtomicInteger sessionid = new AtomicInteger(1);
    @Override
    protected ClientSession createNewSession() {
        MNClientSession session = Actors.AsActor(MNClientSession.class, clientScheduler );
        session.$init(this,sessionid.incrementAndGet());
        return session;
    }

    public static class CmdLine {
        @Parameter(names = {"-port", "-p"}, description = "port to listen")
        Integer port = 8887;

        @Parameter(names = {"-cr", "-contentRoot"}, description = "directory to serve files from")
        String contentRoot = ".";
    }

    public static void main(String[] args) throws Exception {
        CmdLine params = new CmdLine();
        new JCommander(params, args);
        new NettyWSHttpServer(params.port, new MachNetz(new File(params.contentRoot))).run();
    }

}

package org.nustaq.kontraktor.remoting.http.netty.service;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.http.RequestProcessor;
import org.nustaq.kontraktor.remoting.http.RequestResponse;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;

/**
 * Created by ruedi on 18.08.14.
 *
 * Adaptor to use netty as a host for pure / JSon / KSon based actor remoting (plain old http)
 *
 * Use ActorWSServer subclass to also get WebSocketSupport
 *
 */
public class HttpRemotingServer extends WebSocketHttpServer implements NioHttpServer {

    protected NettyWSHttpServer nettyWSHttpServer;
    protected ArrayList<RequestProcessor> processors = new ArrayList<>(); // FIXME: is netty calling this multithreaded ?

    public HttpRemotingServer() {
        super(new File("."));
    }

    public HttpRemotingServer( File contentRoot ) {
        super(contentRoot);
    }

    @Override
    public void onHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, NettyWSHttpServer.HttpResponseSender sender) {
        if (req.getMethod() == HttpMethod.GET || req.getMethod() == HttpMethod.POST) {
            NettyKontraktorHttpRequest kreq = new NettyKontraktorHttpRequest(req);
            boolean processed = false;
            try {
                for (int i = 0; !processed && i < processors.size(); i++) {
                    RequestProcessor processor = processors.get(i);
                    processed = processor.processRequest(kreq, (result, error) -> {
                        // quirksmode as I cannot directly write http header with netty (or did not figure out how to do that)
                        if (result == RequestResponse.MSG_200) {
                            ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.OK));
                            return;
                        }
                        if (result == RequestResponse.MSG_404) {
                            ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.NOT_FOUND));
                            return;
                        }
                        if (result == RequestResponse.MSG_500) {
                            ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.INTERNAL_SERVER_ERROR));
                            return;
                        }
                        if (error == null || error == RequestProcessor.FINISHED) {
                            try {
                                if (result != null) {
                                    ctx.write(Unpooled.copiedBuffer(result.toString(), Charset.forName("UTF-8")));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (error != null) {
                            try {
                                if (error != RequestProcessor.FINISHED) {
                                    ctx.write(Unpooled.copiedBuffer(error.toString(), Charset.forName("UTF-8")));
                                }
                                ChannelFuture f = ctx.writeAndFlush(Unpooled.copiedBuffer("", Charset.forName("UTF-8")));
                                f.addListener((ChannelFuture future) -> future.channel().close());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                if (!processed) {
                    super.onHttpRequest(ctx, req, sender); // falls back to serve file (native netty impl)
                }
            } catch (Exception e) {
                ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.INTERNAL_SERVER_ERROR));
                ctx.writeAndFlush(Unpooled.copiedBuffer(e.getClass().getSimpleName() + ":" + e.getMessage().toString(), Charset.forName("UTF-8")));
                ctx.close();
                e.printStackTrace();
            }
        } else {
            sender.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_0, FORBIDDEN));
        }
    }

    @Override
    public void $init(int port, RequestProcessor restProcessor) {
        nettyWSHttpServer = new NettyWSHttpServer(port, this);
        $addHttpProcessor(restProcessor);
    }

    @Override
    public void $addHttpProcessor(RequestProcessor restProcessor) {
        processors.add(restProcessor);
    }

    @Override
    public void $receive() {
        new Thread( () -> {
            try {
                nettyWSHttpServer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    @Override
    public Actor getServingActor() {
        return null;
    }

    @Override
    protected ClientSession createNewSession() {
        return null;
    }

//    public static void main(String[] args) throws Exception {
//        RestActorServer sv = new RestActorServer().map(RestActorServer.MDesc.class);
//        sv.publish("rest",Actors.AsActor(RestActorServer.RESTActor.class,65000));
//        sv.startOnServer(9999, new KontraktorNettyServer());
//    }

}

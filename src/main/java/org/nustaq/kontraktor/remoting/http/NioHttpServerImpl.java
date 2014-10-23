package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 13.08.2014.
 *
 * Minimalistic Http server implementation. Only partial Http implementation necessary for Kontraktor Http Remoting.
 * Do NOT use in production. At least requires a reverse proxy like NGINX in front to have get some protection.
 * Main purpose is development/light weight inhouse usage. Avoids dependencies of kontraktor-core.
 *
 * Its recommended to use KontraktorNettyServer (see github/kontraktor) for production apps.
 */
public class NioHttpServerImpl extends Actor<NioHttpServerImpl> implements NioHttpServer {

    ServerSocketChannel socket;
    Selector selector;
    SelectionKey serverkey;
    ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
    int port;
    RequestProcessor processor;
    boolean shouldTerminate = false;
    long lastRequest;

    public void $init( int port, RequestProcessor processor) {
        Thread.currentThread().setName("NioHttp");
        this.port = port;
        this.processor = processor;
        try {
            selector = Selector.open();
            socket = ServerSocketChannel.open();
            socket.socket().bind(new java.net.InetSocketAddress(port));
            socket.configureBlocking(false);
            serverkey = socket.register(selector, SelectionKey.OP_ACCEPT);

            info("bound to port " + port);
        } catch (IOException e) {
            severe("could not bind to port" + port);
            Log.Lg.error(this,e,null);
        }
    }

    protected void severe(String s) {
        Log.Lg.error(this,null,s);
    }

    protected void info(String s) {
        Log.Info(this,s);
    }

    public void $receive() {
        try {
            selector.selectNow();
            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                SelectionKey key = iterator.next();
                try {
                    if (key == serverkey) {
                        if (key.isAcceptable()) {
                            SocketChannel accept = socket.accept();
                            if (accept != null) {
                                accept.configureBlocking(false);
                                accept.register(selector, SelectionKey.OP_READ);
                                lastRequest = System.currentTimeMillis();
                            }
                        }
                    } else {
                        SocketChannel client = (SocketChannel) key.channel();
                        if (key.isReadable()) {
                            iterator.remove();
                            try {
                                service(key, client);
                            } catch (IOException ioe) {
                                key.cancel();
                                client.close();
                                throw ioe;
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.Warn(this,e,"");
                }
            }
        } catch (Throwable e) {
            Log.Warn(this,e,"");
        }
        if ( ! shouldTerminate ) {
            if ( System.currentTimeMillis() - lastRequest > 100 ) {
                LockSupport.parkNanos(1000 * 1000); // latency max 1 ms, but avoid excessive power consumption.
            }
            self().$receive();
        }
    }

    @Override @CallerSideMethod
    public Actor getServingActor() {
        return this;
    }

    @Override
    public void $addHttpProcessor(RequestProcessor restProcessor) {
        if ( processor != null && restProcessor != processor)
            Log.Warn(this, "httpprocessor already set");
        processor = restProcessor;
    }

    public void $stopService() {
        shouldTerminate = true;
    }

    RateMeasure reqPerS = new RateMeasure("req/s", 5000);
    protected void service(final SelectionKey key, final SocketChannel client) throws IOException {
        if (!client.isOpen()) {
            key.cancel();
            client.close();
            return;
        }
        int bytesread = client.read(buffer);
        if (bytesread == -1) {
            key.cancel();
            client.close();
        } else {
            buffer.flip();
            reqPerS.count();
            KontraktorHttpRequest request = (KontraktorHttpRequest) key.attachment();
            if (request==null) {
                request = new KontraktorHttpRequestImpl(buffer, bytesread);
            }
            else {
                request.append(buffer, bytesread);
            }
            if ( ! request.isComplete() ) {
                key.attach(request);
            } else {
                key.attach(null);
                if (processor != null) {
                    try {
                        processor.processRequest(request,
                        (result, error) -> {

                             if (error == null || error == RequestProcessor.FINISHED) {
                                 try {
                                     if (result != null) {
                                         writeClient(client, ByteBuffer.wrap(result.getBinary()));
                                     }
                                 } catch (Exception e) {
                                     Log.Warn(this,e,"");
                                 }
                             }
                             if (error != null) {
                                 try {
                                     if (error != RequestProcessor.FINISHED) {
                                         if ( error instanceof Throwable ) {
                                             writeClient(
                                                 client,
                                                 ByteBuffer.wrap(FSTUtil.toString(((Throwable) error)).getBytes())
                                              );
                                         } else
                                            writeClient(client, ByteBuffer.wrap(error.toString().getBytes()));
                                     }
                                     key.cancel();
                                     client.close();
                                 } catch (IOException e) {
                                     Log.Warn(this,e,"");
                                 }
                             }
                        });
                    } catch (Exception ex) {
                        writeClient(client, ByteBuffer.wrap(FSTUtil.toString((ex)).getBytes()));
                        key.cancel();
                        client.close();
                    }
                } else {
                    key.cancel();
                    try {
                        client.close();
                    } catch (IOException e) {
                        Log.Warn(this,e,"");
                    }
                }
            }
            buffer.clear();
        }
    }

    private void writeClient(SocketChannel client, ByteBuffer wrap) throws IOException {
        while ( wrap.remaining() > 0 )
            client.write(wrap);
    }

    static class SimpleProcessor implements RequestProcessor {

        @Override
        public boolean processRequest(KontraktorHttpRequest req, Callback response) {
            response.receive(new RequestResponse("HTTP/1.0 200 OK\nAccess-Control-Allow-Origin: *\n\n" + req.getText()), null);
            response.receive(null, FINISHED);
            return true;
        }
    }

    public static void main( String arg[] ) throws InterruptedException {
        NioHttpServerImpl server = Actors.AsActor(NioHttpServerImpl.class);
        server.$init(9999, new SimpleProcessor());
        server.$receive();
    }

}


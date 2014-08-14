package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;

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
 */
public class NioHttpServer extends Actor<NioHttpServer> {

    ServerSocketChannel socket;
    Selector selector;
    SelectionKey serverkey;
    ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
    int port;
    RequestProcessor processor;
    boolean shouldTerminate = false;
    long lastRequest;

    public void $init( int port, RequestProcessor processor) {
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
            e.printStackTrace();
        }
    }

    protected void severe(String s) {
        System.out.println("SEVERE:"+s);
    }

    protected void info(String s) {
        System.out.println("INFO:"+s);
    }

    public void $receive() {
        try {
            int keys = selector.selectNow();
            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                SelectionKey key = iterator.next();
                try {
                    if (key == serverkey) {
                        if (key.isAcceptable()) {
                            SocketChannel accept = socket.accept();
                            if (accept != null) {
                                accept.configureBlocking(false);
                                SelectionKey register = accept.register(selector, SelectionKey.OP_READ);
                                register.attach(0);
                                lastRequest = System.currentTimeMillis();
                            }
                        }
                    } else {
                        SocketChannel client = (SocketChannel) key.channel();
                        if (key.isReadable()) {
                            iterator.remove();
                            service(key, client);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if ( ! shouldTerminate ) {
            if ( System.currentTimeMillis() - lastRequest > 100 ) {
                LockSupport.parkNanos(1000 * 1000); // latency ..
            }
            self().$receive();
        }
    }

    public void $stopService() {
        shouldTerminate = true;
    }

    protected void service(final SelectionKey key, final SocketChannel client) throws IOException {
        int bytesread = client.read(buffer);
        if (bytesread == -1) {
            key.cancel();
            client.close();
        } else {
            buffer.flip();
            KontraktorHttpRequest request = decode(buffer,bytesread);
            System.out.println("=>");
            System.out.println(request.getText());
            System.out.println("<=");
            if (processor!=null) {
                processor.processRequest(request,
                        (result, error) -> {
                            if ( error == null || error == RequestProcessor.FINISHED ) {
                                try {
                                    client.write(ByteBuffer.wrap(result.toString().getBytes())); //fixme
                                    key.attach((int) key.attachment() + 1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (error != null) {
                                try {
                                    if ( error != RequestProcessor.FINISHED ) {
                                        result = RequestResponse.MSG_500;
                                        client.write(ByteBuffer.wrap(RequestResponse.MSG_500.toString().getBytes())); //fixme
                                    }
                                    key.cancel();
                                    client.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
            } else {
                key.cancel();
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            buffer.clear();
        }
    }

    protected KontraktorHttpRequest decode(ByteBuffer buffer, int len) {
        return new KontraktorHttpRequest(buffer,len);
    }

    static class SimpleProcessor implements RequestProcessor {

        @Override
        public void processRequest(KontraktorHttpRequest req, Callback response) {
            response.receiveResult("HTTP/1.0 200 OK\n\n"+req.getText(), null);
            response.receiveResult(null,FINISHED);
        }
    }

    public static void main( String arg[] ) throws InterruptedException {
        NioHttpServer server = Actors.AsActor(NioHttpServer.class);

        server.$init(9999, new SimpleProcessor());
        server.$receive();
    }

}


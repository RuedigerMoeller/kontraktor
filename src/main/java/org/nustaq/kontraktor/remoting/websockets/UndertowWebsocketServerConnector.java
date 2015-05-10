package org.nustaq.kontraktor.remoting.websockets;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.serialization.FSTConfiguration;
import org.xnio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public class UndertowWebsocketServerConnector implements ActorServerConnector {

    public static Promise<ActorServer> Publish(Actor facade, String host, String path, int port, Coding coding) {
        Promise finished = new Promise();
        try {
            ActorServer publisher = new ActorServer(new UndertowWebsocketServerConnector(path,port,host), facade, coding);
            facade.execute(() -> {
                try {
                    publisher.start();
                    finished.resolve(publisher);
                } catch (Exception e) {
                    finished.reject(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise(null,e);
        }
        return finished;
    }

    String host;
    String path;
    int port;
    Undertow server;

    public UndertowWebsocketServerConnector(String path, int port, String host) {
        this.path = path;
        this.port = port;
        this.host = host;
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        PathHandler server = createServer();
        server.addExactPath(
            path,
            Handlers.websocket( (exchange, channel) -> { // connection callback
                Runnable runnable = () -> {
                    UTWebObjectSocket objectSocket = new UTWebObjectSocket(exchange, channel);
                    ObjectSink sink = factory.apply(objectSocket);

                    channel.getReceiveSetter().set(new AbstractReceiveListener() {
                        @Override
                        protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            sink.sinkClosed();
                        }

                        @Override
                        protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                            ByteBuffer[] data = message.getData().getResource();
                            byte[] bytez = Buffers.take(data, 0, data.length);
                            sink.receiveObject(objectSocket.getConf().asObject(bytez));
                        }
                    });
                };
//                runnable.run();
                facade.execute(runnable);
                channel.resumeReceives();
            })
        );
    }

    protected PathHandler createServer() {
        PathHandler pathHandler = new PathHandler();
        server = Undertow.builder()
                   .setIoThreads( 2 )
                   .setWorkerThreads( 2 )
                   .addHttpListener( port, host )
                   .setHandler(pathHandler)
                   .build();
        server.start();
        return pathHandler;
    }

    @Override
    public IPromise closeServer() {
        server.stop();
        return new Promise();
    }

    static class UTWebObjectSocket implements ObjectSocket {

        WebSocketChannel channel;
        WebSocketHttpExchange ex;
        ArrayList objects = new ArrayList();
        FSTConfiguration conf;
        Throwable lastError;

        public UTWebObjectSocket(WebSocketHttpExchange ex, WebSocketChannel channel) {
            this.ex = ex;
            this.channel = channel;
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            objects.add(toWrite);
            if (objects.size() > 100 ) {
                flush();
            }
        }

        @Override
        public void flush() throws Exception {
            if ( objects.size() == 0 ) {
                return;
            }
            objects.add(0); // sequence
            Object[] objArr = objects.toArray();
            objects.clear();
            // fixme: catche exception thrown on close
            WebSockets.sendBinary(ByteBuffer.wrap( conf.asByteArray(objArr)), channel, new WebSocketCallback() {
                @Override
                public void complete(WebSocketChannel channel, Object context) {
                    // FIXME: manage async write
                }

                @Override
                public void onError(WebSocketChannel channel, Object context, Throwable throwable) {
                    setLastError(throwable);
                }
            });
        }

        @Override
        public void setLastError(Throwable ex) {
            lastError = ex;
        }

        @Override
        public Throwable getLastError() {
            return lastError;
        }

        @Override
        public void setConf(FSTConfiguration conf) {
            this.conf = conf;
        }

        @Override
        public FSTConfiguration getConf() {
            return conf;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}

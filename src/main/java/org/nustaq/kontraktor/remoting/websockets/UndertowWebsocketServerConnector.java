package org.nustaq.kontraktor.remoting.websockets;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Pair;
import org.xnio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // a map of port=>server also used by httpconnector
    public static Map<Integer, Pair<PathHandler,Undertow>> serverMap = new HashMap<>();

    public static synchronized Pair<PathHandler, Undertow> GetServer(int port, String hostName) {
        Pair<PathHandler, Undertow> pair = serverMap.get(port);
        if (pair == null) {
            PathHandler pathHandler = new PathHandler();
            Undertow server = Undertow.builder()
                    .setIoThreads( 2 )
                    .setWorkerThreads( 2 )
                    .addHttpListener( port, hostName)
                    .setHandler(pathHandler)
                    .build();
            server.start();
            pair = new Pair<>(pathHandler,server);
            serverMap.put(port,pair);
        }
        return pair;
    }

    public UndertowWebsocketServerConnector(String path, int port, String host) {
        this.path = path;
        this.port = port;
        this.host = host;
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        PathHandler server = getServer(port).getFirst();
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

    protected Pair<PathHandler, Undertow> getServer(int port) {
        String hostName = this.host;
        return GetServer(port, hostName);
    }

    @Override
    public IPromise closeServer() {
        getServer(port).getSecond().stop();
        return new Promise(null);
    }

    static class UTWebObjectSocket extends WebObjectSocket {

        WebSocketChannel channel;
        WebSocketHttpExchange ex;

        public UTWebObjectSocket(WebSocketHttpExchange ex, WebSocketChannel channel) {
            this.ex = ex;
            this.channel = channel;
        }

        @Override
        public void sendBinary(byte[] message) {
            WebSockets.sendBinary(ByteBuffer.wrap(message), channel, new WebSocketCallback() {
                @Override
                public void complete(WebSocketChannel channel, Object context) {
                    // FIXME: manage partial write ?
                }

                @Override
                public void onError(WebSocketChannel channel, Object context, Throwable throwable) {
                    setLastError(throwable);
                }
            });
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}

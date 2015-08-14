/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

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
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.serialization.util.FSTUtil;
import org.xnio.Buffers;
import java.io.IOException;
import java.lang.ref.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 *
 * Publishes an actor as a websocket server using Undertow.
 *
 */
public class UndertowWebsocketServerConnector implements ActorServerConnector {

    String host;
    String path;
    int port;
    boolean sendStrings = false;

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
            Handlers.websocket((exchange, channel) -> { // connection callback
               Runnable runnable = () -> {
                   UTWebObjectSocket objectSocket = new UTWebObjectSocket(exchange, channel, sendStrings);
                   ObjectSink sink = factory.apply(objectSocket);
                   objectSocket.setSink(sink);

                   channel.getReceiveSetter().set(new AbstractReceiveListener() {
                       @Override
                       protected void onCloseMessage(CloseMessage cm, WebSocketChannel channel) {
                           try {
                               channel.close();
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                           sink.sinkClosed();
                           try {
                               objectSocket.close();
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                           channel.getReceiveSetter().set(null);
                       }

                       @Override
                       protected void onError(WebSocketChannel channel, Throwable error) {
                           Log.Debug(this,error);
                           sink.sinkClosed();
                       }

                       @Override
                       protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                           String data = message.getData();
                           byte[] bytez = data.getBytes("UTF-8");
                           sink.receiveObject(objectSocket.getConf().asObject(bytez), null);
                       }

                       @Override
                       protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                           ByteBuffer[] data = message.getData().getResource();
                           byte[] bytez = Buffers.take(data, 0, data.length);
                           sink.receiveObject(objectSocket.getConf().asObject(bytez), null);
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
        return Http4K.get().getServer(port, hostName);
    }

    @Override
    public IPromise closeServer() {
        getServer(port).getSecond().stop();
        return new Promise(null);
    }

    public UndertowWebsocketServerConnector sendStrings(final boolean sendStrings) {
        this.sendStrings = sendStrings;
        return this;
    }


    protected static class UTWebObjectSocket extends WebObjectSocket {

        protected final boolean sendStrings;
        protected WebSocketChannel channel;
        protected WebSocketHttpExchange ex;
        protected WeakReference<ObjectSink> sink;

        public UTWebObjectSocket(WebSocketHttpExchange ex, WebSocketChannel channel, boolean sendStrings) {
            this.ex = ex;
            this.channel = channel;
            this.sendStrings = sendStrings;
        }

        @Override
        public void sendBinary(byte[] message) {
            WebSocketCallback callback = new WebSocketCallback() {
                @Override
                public void complete(WebSocketChannel channel, Object context) {
                }

                @Override
                public void onError(WebSocketChannel channel, Object context, Throwable throwable) {
                    setLastError(throwable);
                    try {
                        isClosed = true;
                        UTWebObjectSocket.this.close();
                    } catch (IOException e) {
                        FSTUtil.<RuntimeException>rethrow(e);
                    }
                }
            };
            if ( sendStrings ) { // node filereader cannot handle blobs
                WebSockets.sendText(ByteBuffer.wrap(message), channel, callback );
            } else {
                WebSockets.sendBinary(ByteBuffer.wrap(message), channel, callback);
            }
        }

        @Override
        public void close() throws IOException {
            channel.getReceiveSetter().set(null);
            channel.close();
            ObjectSink objectSink = sink.get();
            if (objectSink != null )
                objectSink.sinkClosed();
            conf = null;
            channel = null;
            ex = null;
        }

        static AtomicInteger idCount = new AtomicInteger(0);
        int id = idCount.incrementAndGet();
        @Override
        public int getId() {
            return id;
        }

        public void setSink(ObjectSink sink) {
            this.sink = new WeakReference<ObjectSink>(sink);
        }

        public ObjectSink getSink() {
            return sink.get();
        }
    }
}

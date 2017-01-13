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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.codec.binary.Base64;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.http.ProxySettings;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;
import org.xnio.Cancellable;
import org.xnio.ChannelListener;
import org.xnio.FutureResult;
import org.xnio.IoFuture;
import org.xnio.StreamConnection;
import org.xnio.http.HttpUpgrade;

import io.undertow.UndertowMessages;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.client.WebSocketClientHandshake;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.jsr.ServerWebSocketContainer;

/**
 * Created by ruedi on 10/05/15.
 *
 * Connects to a websocket-published actor using JSR356 websocket API
 *
 */
public class JSR356ClientConnector implements ActorClientConnector {

    public static boolean DumpProtocol = false;

    public static class RemotingHelper extends Actor<RemotingHelper> {}
    static AtomicReference<RemotingHelper> singleton =  new AtomicReference<>();

    /**
     * in case clients are connected from non actor world, provide a global actor(thread) for remote client processing
     * (=polling queues, encoding)
     */
    static RemotingHelper get() {
        synchronized (singleton) {
            if ( singleton.get() == null ) {
                singleton.set(Actors.AsActor(RemotingHelper.class));
            }
            return singleton.get();
        }
    }

    WSClientEndpoint endpoint;
    URI uri;

    public JSR356ClientConnector(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
    }

    @Override
    public IPromise connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        IPromise connected = new Promise();
        endpoint = new WSClientEndpoint(uri,null, connected);
        ObjectSink sink = factory.apply(endpoint);
        endpoint.setSink(sink);
        return connected;
    }

    @Override
    public IPromise closeClient() {
        try {
            endpoint.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new Promise<>(null,e);
        }
        return new Promise<>(null);
    }

    @ClientEndpoint
    protected static class WSClientEndpoint extends WebObjectSocket {
        static AtomicInteger idCount = new AtomicInteger(0);
        int id = idCount.incrementAndGet();

        protected ObjectSink sink;
        protected volatile Session session = null;
        private volatile IPromise connected;

        public WSClientEndpoint(URI endpointURI, ObjectSink sink, IPromise connected) {
            this.connected = connected;
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                ProxySettings proxySettings = ProxySettings.getProxySettings();
                if (proxySettings != null) {
                    ServerWebSocketContainer serverWebSocketContainer = (ServerWebSocketContainer)container;
                    ConnectionBuilder connectionBuilder = null;
                    if (proxySettings.getProxyUser() != null) {
                        connectionBuilder = new ProxyWithAuthenticationUndertowConnectionBuilder(serverWebSocketContainer.getXnioWorker(), serverWebSocketContainer.getBufferPool(), endpointURI, proxySettings.getProxyUser(), proxySettings.getProxyPassword());
                    } else {
                        connectionBuilder = new ConnectionBuilder(serverWebSocketContainer.getXnioWorker(), serverWebSocketContainer.getBufferPool(), endpointURI);
                    }
                    connectionBuilder.setProxyUri(new URI("http", null, proxySettings.getProxy(), proxySettings.getProxyPort(), "/", null, null));
                    serverWebSocketContainer.connectToServer(this, connectionBuilder);
                } else {
                    container.connectToServer(this, endpointURI);
                }
                this.sink = sink;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ObjectSink getSink() {
            return sink;
        }

        public void setSink(ObjectSink sink) {
            this.sink = sink;
        }

        @OnOpen
        public void onOpen(Session userSession) {
            this.session = userSession;
            connected.complete();
        }

        @OnClose
        public void onClose(Session userSession, CloseReason reason) {
            this.session = null;
            Log.Info(this, "Connection closed " + reason);
            sink.sinkClosed();
        }

        @OnError
        public void onError( Throwable th ) {
            Log.Warn(this,th);
        }

        @OnMessage
        public void onMessage(byte[] message) {
            if ( DumpProtocol ) {
                System.out.println("resp:");
                System.out.println(new String(message,0));
            }
            sink.receiveObject(conf.asObject(message), null);
        }

        @OnMessage
        public void onTextMessage(String message) {
            if ( DumpProtocol ) {
                System.out.println("resp:");
                System.out.println(message);
            }
            sink.receiveObject(conf.asObject(message.getBytes() /*is already utf 8*/), null);
        }

        public void sendText(String message) {
            session.getAsyncRemote().sendText(message);
        }

        public void sendBinary(byte[] message) {
            if ( DumpProtocol ) {
                System.out.println("requ:");
                System.out.println(new String(message,0));
            }
            Actor executor = Actor.current();
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message), new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    if ( ! result.isOK() ) {
                        executor.execute( () -> {
                            FSTUtil.<RuntimeException>rethrow(result.getException());
                            try {
                                close();
                            } catch (IOException e) {
                                Log.Warn(this,e);
                            }
                        });
                    }
                }
            });
        }

        public void close() throws IOException {
            if ( session != null )
                session.close();
            sink.sinkClosed();
        }

        @Override
        public int getId() {
            return id;
        }

    }

}


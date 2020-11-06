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

import afu.org.checkerframework.checker.units.qual.A;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
        endpoint = new WSClientEndpoint(uri,null);
        ObjectSink sink = factory.apply(endpoint);
        endpoint.setSink(sink);

        return new Promise<>(null);
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

        public WSClientEndpoint(URI endpointURI, ObjectSink sink) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, endpointURI);
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
            Object received = conf.asObject(message);
//            System.out.println("JSR REC "+count.get());
            sink.receiveObject(received, null, null );
        }

        @OnMessage
        public void onTextMessage(String message) {
            if ( DumpProtocol ) {
                System.out.println("resp:");
                System.out.println(message);
            }
            sink.receiveObject(conf.asObject(message.getBytes() /*is already utf 8*/), null, null );
        }

        public void sendText(String message) {
            session.getAsyncRemote().sendText(message);
        }

        public void sendBinary(byte[] message) {
            if ( DumpProtocol ) {
                System.out.println("requ:");
                System.out.println(new String(message,0));
            }
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // UNDERTOW 2.2.2 Bug with async send.
//            Actor executor = Actor.current();
//            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message), new SendHandler() {
//                @Override
//                public void onResult(SendResult result) {
//                    senRec.decrementAndGet();
//                    System.out.println("SENDREC "+senRec);
//                    if ( ! result.isOK() ) {
//                        executor.execute( () -> {
//                            FSTUtil.<RuntimeException>rethrow(result.getException());
//                            try {
//                                close();
//                            } catch (IOException e) {
//                                Log.Warn(this,e);
//                            }
//                        });
//                    }
//                }
//            });
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

        @Override
        public String getConnectionIdentifier() {
            return ""+id;
        }

    }

}


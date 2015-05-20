package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 *
 * Connects to a websocket-published actor using JSR356 websocket API
 *
 */
public class JSR356ClientConnector implements ActorClientConnector {


    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String url, Coding c ) {
        Promise result = new Promise();
        Runnable connect = () -> {
            JSR356ClientConnector client = null;
            try {
                client = new JSR356ClientConnector(url);
                ActorClient connector = new ActorClient(client,clz,c);
                connector.connect().then(result);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                result.reject(e);
            }
        };
        if ( ! Actor.inside() ) {
            get().execute(() -> Thread.currentThread().setName("singleton remote client actor polling"));
            get().execute(connect);
        }
        else
            connect.run();
        return result;
    }

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
    public void connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        endpoint = new WSClientEndpoint(uri,null);
        ObjectSink sink = factory.apply(endpoint);
        endpoint.setSink(sink);
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

        @OnMessage
        public void onMessage(byte[] message) {
            sink.receiveObject(conf.asObject(message), null);
        }

        @OnMessage
        public void onTextMessage(String message) {
        }

        public void sendText(String message) {
            session.getAsyncRemote().sendText(message);
        }

        public void sendBinary(byte[] message) {
            Actor executor = Actor.current();
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message), new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    if ( ! result.isOK() ) {
                        executor.execute( () -> {
                            Actors.throwException(result.getException());
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

    }

}


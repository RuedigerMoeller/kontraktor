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
import org.nustaq.serialization.FSTConfiguration;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public class WebsocketAPIClientConnector implements ActorClientConnector {


    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String url, Coding c ) {
        Promise result = new Promise();
        Runnable connect = () -> {
            WebsocketAPIClientConnector client = null;
            try {
                client = new WebsocketAPIClientConnector(url);
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

    public WebsocketAPIClientConnector( String uri ) throws URISyntaxException {
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
    protected static class WSClientEndpoint implements ObjectSocket {

        protected ObjectSink sink;
        protected volatile Session session = null;
        protected Throwable lastError;
        protected FSTConfiguration conf;
        protected ArrayList objects = new ArrayList();

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
            sink.receiveObject(conf.asObject(message));
        }

        @OnMessage
        public void onTextMessage(String message) {
        }

        public void sendText(String message) {
            session.getAsyncRemote().sendText(message);
        }

        public void sendBinary(byte[] message) {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message), new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    // FIXME:
                }
            });
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            objects.add(toWrite);
            if ( objects.size() > 100 ) {
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
            sendBinary(conf.asByteArray(objArr));
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

        public void close() throws IOException {
            if ( session != null )
                session.close();
            sink.sinkClosed();
        }

    }

}


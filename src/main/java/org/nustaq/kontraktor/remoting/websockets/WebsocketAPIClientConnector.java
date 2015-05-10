package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public class WebsocketAPIClientConnector implements ActorClientConnector {

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
            sendBinary(conf.asByteArray(toWrite));
        }

        @Override
        public void flush() throws Exception {
            // batching not implemented for now
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


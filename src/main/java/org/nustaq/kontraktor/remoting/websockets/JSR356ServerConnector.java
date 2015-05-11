package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by ruedi on 11/05/15.
 *
 * Currently does not work with undertow (spec drama). Have not tried with other impls. For now use UndertowWebsocketServerConnector
 *
 */
@ServerEndpoint("ws")
public class JSR356ServerConnector extends Endpoint implements ActorServerConnector {

    Actor facade;
    Function<ObjectSocket, ObjectSink> factory;

    public static IPromise<ActorServer> Publish( Actor facade, String path, Coding coding) {
        JSR356ServerConnector connector = new JSR356ServerConnector();
        try {
            ActorServer actorServer = new ActorServer(connector, facade, coding);
            actorServer.start();
            ContainerProvider.getWebSocketContainer().connectToServer(connector, /*new DefaultClientEndpointConfig(),*/ new URI(path));
            return new Promise<>(actorServer);
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise<>(null,e);
        }
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        this.facade = facade;
        this.factory = factory;
    }

    @Override
    public IPromise closeServer() {
        return new Promise<>(null,"unable to close from here, must stop hosting server instead");
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        MySocket objectsocket = new MySocket(session);
        ObjectSink sink = factory.apply(objectsocket);
        session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte msg[]) {
                sink.receiveObject(msg);
            }
        });
    }

    static class MySocket extends WebObjectSocket {

        Session session;

        public MySocket(Session session) {
            this.session = session;
        }

        @Override
        public void sendBinary(byte[] message) {
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
            } catch (IOException ex) {
                Log.Warn(this, ex);
                try {
                    close();
                } catch (IOException e) {
                    Log.Warn(this, ex);
                }
            }
        }

        @Override
        public void close() throws IOException {
            session.close();
        }
    }

    static class DefaultClientEndpointConfig implements ClientEndpointConfig {

        @Override
        public List<Class<? extends Encoder>> getEncoders() {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends Decoder>> getDecoders() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> getUserProperties() {
            return Collections.emptyMap();
        }

        @Override
        public List<String> getPreferredSubprotocols() {
            return Collections.emptyList();
        }

        @Override
        public List<Extension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public Configurator getConfigurator() {
            return new Configurator();
        }
    }

}

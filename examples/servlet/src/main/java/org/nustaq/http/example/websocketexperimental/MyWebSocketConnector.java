package org.nustaq.http.example.websocketexperimental;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.nustaq.http.example.ServletApp;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

/**
 * Created by ruedi on 26.06.17.
 *
 */
//@ServerEndpoint("/ws")
public class MyWebSocketConnector extends Endpoint implements ActorServerConnector {

    private static volatile MyWebSocketConnector _instance;
    
    private Map<String, ServletWebObjectSocket> sessions = new HashMap<>();
    
    ServletApp facade;
    Function<ObjectSocket, ObjectSink> sinkFactory;

    public MyWebSocketConnector() {
        System.out.println("creating jee websocket connector");
        System.out.println("init ws");
        ActorServer actorServer = null;
        facade = Actors.AsActor(ServletApp.class);
        facade.init(new BasicWebAppConfig());

        try {
            actorServer = new ActorServer( this, facade, new Coding(SerializerType.JsonNoRef) );
//            wscon.setActorServer(actorServer);
            actorServer.start( actor -> Log.Warn(this,"disconnected "+actor));
        } catch (Exception e) {
            Log.Error(this,e);
        }
    }

    public static MyWebSocketConnector getInstance() {
        MyWebSocketConnector localInstance = _instance;
        if (localInstance == null) {
            synchronized (MyWebSocketConnector.class) {
                localInstance = _instance;
                if (localInstance == null) {
                    _instance  = localInstance = new MyWebSocketConnector();
                }
            }
        }
        return localInstance;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println(session.getId() + " has opened a connection");

        // never received in tcat 8.5 ??
//        session.addMessageHandler(byte[].class, message -> {
//                System.out.println("msg byte[] "+new String(message,0));
//            }
//        );
        ServletWebObjectSocket socket = new ServletWebObjectSocket(session);
        sessions.put(session.getId(), socket);
        facade.execute(() -> {
            ObjectSink sink = sinkFactory.apply(socket);
            socket.setSink(sink);
            session.addMessageHandler(String.class, message -> {
                    System.out.println("msg String "+message);
                    try {
                        Object o = socket.getConf().asObject(message.getBytes("UTF-8"));
                        socket.getSink().receiveObject(o, null, null );
                    } catch (UnsupportedEncodingException e) {
                        Log.Warn(this,e);
                    }
            });
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        closeSession(session);
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        super.onError(session, throwable);
        closeSession(session);
    }
    
    private void closeSession(Session session) {
        try {
            ServletWebObjectSocket socket = sessions.remove(session.getId());
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        this.facade = (ServletApp) facade;
        this.sinkFactory = factory;
    }

    @Override
    public IPromise closeServer() {
        return null;
    }

    protected static class ServletWebObjectSocket extends WebObjectSocket {

        protected Session session;
        protected ObjectSink sink;

        public ServletWebObjectSocket(Session channel) {
            super();
            this.session = channel;
        }

        @Override
        public void sendBinary(byte[] message) {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message));
        }

        @Override
        public void close() throws IOException {
//            session.getReceiveSetter().set(null);
            session.close();
            ObjectSink objectSink = sink;
            if (objectSink != null )
                objectSink.sinkClosed();
            conf = null;
            session = null;
        }

        static AtomicInteger idCount = new AtomicInteger(0);
        int id = idCount.incrementAndGet();
        @Override
        public int getId() {
            return id;
        }

        public void setSink(ObjectSink sink) {
            this.sink = sink;
        }

        public ObjectSink getSink() {
            return sink;
        }
    }

}
package org.nustaq.http.example;

import java.io.UnsupportedEncodingException;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets._JSR356ServerConnector;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

/**
 * Created by ruedi on 26.06.17.
 *
 * tomcat somehow does not detect this ...
 */
//@ServerEndpoint("/ws")
public class MyWebSocketConnector extends _JSR356ServerConnector {
    
    private static volatile MyWebSocketConnector instance;
    
    public MyWebSocketConnector() {
        createAndInitFacadeApp();
        createAndInitConnector();
    }

    public static MyWebSocketConnector getInstance() {
        MyWebSocketConnector localInstance = instance;
        if (localInstance == null) {
            synchronized (MyWebSocketConnector.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance  = localInstance = new MyWebSocketConnector();
                }
            }
        }
        return localInstance;
    }

    protected void createAndInitFacadeApp() {
        facade = Actors.AsActor(ServletApp.class);
        ((ServletApp) facade).init(new BasicWebAppConfig());
    }

    protected void createAndInitConnector() {
        try {
            facade.setThrowExWhenBlocked(true);
            actorServer = new ActorServer(this, facade, new Coding(SerializerType.JsonNoRef));
            actorServer.start(null);
        } catch (Exception e) {
            Log.Error(null, e);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        Log.Info(this,"onOpen "+session+" "+config);
        final MySocket objectsocket = new MySocket(session);
        facade.execute(() -> {
            ObjectSink sink = factory.apply(objectsocket);
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String msg) {
                    byte[] bytez = new byte[0];
                    try {
                        bytez = msg.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    sink.receiveObject(objectsocket.getConf().asObject(bytez), null, null );
                }
            });
        });
    }
}

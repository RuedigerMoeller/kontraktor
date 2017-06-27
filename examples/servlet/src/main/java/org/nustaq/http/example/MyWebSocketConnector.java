package org.nustaq.http.example;

import java.io.IOException;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.nustaq.kontraktor.remoting.websockets._JSR356ServerConnector;

/**
 * Created by ruedi on 26.06.17.
 *
 * tomcat somehow does not detect this ...
 */
//@ServerEndpoint("/ws")
public class MyWebSocketConnector extends _JSR356ServerConnector {

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println(session.getId() + " has opened a connection");
        try {
            session.getBasicRemote().sendText("Connection Established");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

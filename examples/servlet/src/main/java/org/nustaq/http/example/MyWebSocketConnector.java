package org.nustaq.http.example;

import org.nustaq.kontraktor.remoting.websockets._JSR356ServerConnector;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * Created by ruedi on 26.06.17.
 *
 * tomcat somehow does not detect this ...
 */
@ServerEndpoint("/ws")
public class MyWebSocketConnector /*extends _JSR356ServerConnector*/ {

    @OnOpen
    public void onOpen(Session session){
        System.out.println(session.getId() + " has opened a connection");
        try {
            session.getBasicRemote().sendText("Connection Established");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

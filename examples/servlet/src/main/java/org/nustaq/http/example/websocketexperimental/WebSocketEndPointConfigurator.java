package org.nustaq.http.example.websocketexperimental;

import org.nustaq.http.example.websocketexperimental.MyWebSocketConnector;

import javax.websocket.server.ServerEndpointConfig;

/**
 * Singleton Endpoint configurator
 */
public class WebSocketEndPointConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (MyWebSocketConnector.class.equals(endpointClass)) {
            return (T) MyWebSocketConnector.getInstance();
        } else {
            throw new InstantiationException();
        }
    }
}
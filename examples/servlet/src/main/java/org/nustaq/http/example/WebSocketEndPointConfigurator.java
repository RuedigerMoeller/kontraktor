package org.nustaq.http.example;

import javax.websocket.server.ServerEndpointConfig;

/**
 * Singleton Endpoint configurator
 */
public class WebSocketEndPointConfigurator extends ServerEndpointConfig.Configurator {

    private static final MyWebSocketConnector ENDPOINT = new MyWebSocketConnector();

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (MyWebSocketConnector.class.equals(endpointClass)) {
            return (T) ENDPOINT;
        } else {
            throw new InstantiationException();
        }
    }
}
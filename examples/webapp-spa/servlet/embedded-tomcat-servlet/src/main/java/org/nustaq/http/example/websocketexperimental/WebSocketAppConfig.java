package org.nustaq.http.example.websocketexperimental;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;


/**
 *
 */
public class WebSocketAppConfig implements ServerApplicationConfig
{
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses)
    {
        return new HashSet<ServerEndpointConfig>() {{
                add(ServerEndpointConfig.Builder.create(MyWebSocketConnector.class, "/ws")
                        .configurator(new WebSocketEndPointConfigurator()).build());
        }};
    }


    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned)
    {
        return Collections.emptySet();
    }
}
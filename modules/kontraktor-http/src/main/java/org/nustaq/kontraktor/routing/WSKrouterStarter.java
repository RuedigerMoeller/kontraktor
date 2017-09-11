package org.nustaq.kontraktor.routing;

import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.routers.Routing;
import org.nustaq.kontraktor.util.Log;

import java.util.Arrays;

public class WSKrouterStarter {

    public static void main(String[] args) {
        WSKrouterStarterConfig read = WSKrouterStarterConfig.read();
        Arrays.stream( read.getServices() )
            .forEach( routedServiceEntry -> {
                Log.Info(WSKrouterStarter.class,"start "+routedServiceEntry.strategy+":"+routedServiceEntry.encoding+"@"+routedServiceEntry.getPath());
                Routing.start(
                    routedServiceEntry.getStrategy().getClazz(),
                    new WebSocketPublisher()
                        .hostName(read.getHost())
                        .port(read.getPort())
                        .urlPath(routedServiceEntry.getPath())
                        .serType(routedServiceEntry.getEncoding())

                );
            });
    }
}

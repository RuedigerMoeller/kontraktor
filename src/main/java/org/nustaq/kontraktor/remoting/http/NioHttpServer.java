package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;

/**
 * Created by ruedi on 18.08.14.
 */
public interface NioHttpServer {
    void $init(int port, RequestProcessor restProcessor);
    void $receive();
    public Actor getServingActor();
}

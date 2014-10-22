package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;

/**
 * Created by ruedi on 18.08.14.
 */
public interface NioHttpServer {
    void $init(int port, RequestProcessor restProcessor);
    void $receive();
    public Actor getServingActor();
    void $setHttpProcessor(int port, RequestProcessor restProcessor);
}

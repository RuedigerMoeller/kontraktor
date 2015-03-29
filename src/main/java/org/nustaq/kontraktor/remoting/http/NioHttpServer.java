package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;

/**
 * Created by ruedi on 18.08.14.
 */
public interface NioHttpServer {
    void $init(int port, RestProcessor restProcessor);
    void $receive();
    public Actor getServingActor();

    /**
     * set processor for ordinary http requests
     * @param restProcessor
     */
    void $setHttpProcessor(RestProcessor restProcessor);
}

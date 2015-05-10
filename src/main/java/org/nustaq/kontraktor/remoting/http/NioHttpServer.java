package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.ActorServer;

/**
 * Created by ruedi on 18.08.14.
 */
public interface NioHttpServer {

    void addHandler(String path, RestProcessor restProcessor);
    void addHandler(String path, ActorServer webSocketAdapter);
    void removeHandler(String path);

    void startServer();
    void stopServer();

    Actor getServingActor(); // fixme: wrong (more than one) but needed currently by http remoting
}

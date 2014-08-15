package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RestActorClient<T extends Actor> extends RemoteRefRegistry {

    T facadeProxy;
    int port = 9999;
    String host;
    String actorPath;
    Class<T> actorClazz;

    public RestActorClient( String host, int port, String actorPath, Class clz) {
        this.port = port;
        this.host = host;
        this.actorPath = actorPath;
        this.actorClazz = clz;
        facadeProxy = Actors.AsActor(actorClazz, new RemoteScheduler());
        facadeProxy.__remoteId = 1;
        registerRemoteRefDirect(facadeProxy);
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    public void connect() {
        final HttpObjectSocket channel = new HttpObjectSocket(actorClazz, port, host, actorPath);
        new Thread(
            () -> {
                try {
                    sendLoop(channel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            },
            "sender"
        ).start();
        new Thread(
            () -> {
                receiveLoop(channel);
            },
            "receiver"
        ).start();
    }

    public static void main(String a[]) {
        RestActorClient<RestActorServer.RESTActor> cl = new RestActorClient("localhost", 9999, "/rest", RestActorServer.RESTActor.class);
        cl.connect();
        final RestActorServer.RESTActor proxy = cl.getFacadeProxy();
        while( true )
        {
            proxy.simpleCall("A", "B", 133);
            LockSupport.parkNanos(1000*1000*1000l*3);
        }
    }
}

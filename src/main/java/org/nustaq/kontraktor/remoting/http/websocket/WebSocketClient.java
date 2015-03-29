package org.nustaq.kontraktor.remoting.http.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.RemoteConnection;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.tcp.TCPSocket;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.net.SocketException;

/**
 * Created by ruedi on 29/03/15.
 *
 * Connect to a remotely websocket-published actor.
 *
 * FIXME: shares a lot of code. Unify with TCP client/server
 *
 */
public class WebSocketClient<T extends Actor> extends RemoteRefRegistry {

    Class<T> actorClazz;
    T facadeProxy;
    BackOffStrategy backOffStrategy = new BackOffStrategy();

    String host;
    int port;
    ActorClient client;
    volatile boolean connected = false;

    public WebSocketClient(Class<T> clz, String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        actorClazz = clz;
        facadeProxy = Actors.AsActor(actorClazz, new RemoteScheduler());
        facadeProxy.__remoteId = 1;
        registerRemoteRefDirect(facadeProxy);
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    public void connect() throws IOException {
        try {
            client = new ActorClient();
            connected = true;
            facadeProxy.__addRemoteConnection(client);
        } catch (Exception ioe) {
            throw ioe;
        }
    }

    private String getDescriptionString() {
        return actorClazz.getSimpleName() + "@ws://" + host + ":" + port;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     *
     */
    public class ActorClient implements RemoteConnection {

        ObjectSocket chan;

        public ActorClient() throws IOException {
            chan = new TCPSocket(host,port,conf);
            new Thread(
                () -> {
                    currentObjectSocket.set(chan);
                    try {
                        sendLoop(chan);
                    } catch (IOException e) {
                        if (e instanceof SocketException)
                            Log.Lg.infoLong(this,e,"");
                        else
                            Log.Warn(this,e,"");
                    }
                },
                "sender"
            ).start();
            new Thread(
                () -> {
                    currentObjectSocket.set(chan);
                    receiveLoop(chan);
                },
                "receiver"
            ).start();
        }

        public void close() {
            try {
                chan.close();
            } catch (IOException e) {
                Log.Warn(this,e,"");
            }
        }

        @Override
        public void setClassLoader(ClassLoader l) {
            chan.getConf().setClassLoader(l);
        }

        @Override
        public int getRemoteId(Actor act) {
            return WebSocketClient.this.getRemoteId(act);
        }
    }

    @Override
    protected void remoteRefStopped(Actor actor) {
        super.remoteRefStopped(actor);
        if (actor.getActorRef() == facadeProxy.getActorRef() ) {
            // connection closed => close connection and stop all remoteRefs
            setTerminated(true);
            stopRemoteRefs();
            client.close();
        }
    }

}

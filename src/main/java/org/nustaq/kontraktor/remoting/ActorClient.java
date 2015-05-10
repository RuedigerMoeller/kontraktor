package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.RemoteConnection;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 30/03/15.
 */
public abstract class ActorClient<T extends Actor> extends RemoteRefRegistry {

    protected Class<T> actorClazz;
    protected T facadeProxy;
    protected BackOffStrategy backOffStrategy = new BackOffStrategy();

    protected ConnectedActorHandler client;
    protected volatile boolean connected = false;
    protected AtomicReference<OldObjectSocket> objectSocket = new AtomicReference<>(null);

    public ActorClient(Class<T> clz) throws IOException {
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
            objectSocket.set(createObjectSocket());
            client = new ConnectedActorHandler(objectSocket.get());
            connected = true;
            facadeProxy.__addRemoteConnection(client);
        } catch (Exception ioe) {
            throw ioe;
        }
    }

    protected abstract OldObjectSocket createObjectSocket();
//    {
        //return new TCPSocket(host,port,conf);;
//    }

    protected String getDescriptionString() {
        return actorClazz.getSimpleName(); // + "@" + host + ":" + port;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean runReceiveLoop() {
        return true;
    }

    /**
     *
     */
    public class ConnectedActorHandler implements RemoteConnection {

        OldObjectSocket chan;

        protected void sendLoop(OldObjectSocket channel) throws Exception {
            try {
                int count = 0;
                while (!isTerminated()) {
                    if (pollAndSend2Remote(channel)) {
                        count = 0;
                    }
                    backOffStrategy.yield(count++);
                }
            } finally {
                stopRemoteRefs();
            }
        }


        public ConnectedActorHandler(OldObjectSocket socket) throws IOException {
            chan = socket;
            new Thread(
                () -> {
                    try {
                        sendLoop(chan);
                    } catch (Exception e) {
                        if (e instanceof SocketException)
                            Log.Lg.infoLong(this,e,"");
                        else
                            Log.Warn(this,e,"");
                    }
                },
                "sender"
            ).start();

            if (runReceiveLoop()) {
                new Thread(
                    () -> {
                        receiveLoop(chan);
                    },
                    "receiver"
                ).start();
            }
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
            return ActorClient.this.getRemoteId(act);
        }

        @Override
        public void unpublishActor(Actor self) {
            ActorClient.this.unpublishActor(self);
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

    @Override
    public AtomicReference<OldObjectSocket> getObjectSocket() {
        return objectSocket;
    }
}

package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.net.SocketException;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorClient<T extends Actor> extends RemoteRefRegistry {

    public static <T extends Actor> Future<T> Connect( Class<T> clz, String host, int port ) throws IOException {
        Promise<T> res = new Promise<>();
        TCPActorClient<T> client = new TCPActorClient<>( clz, host, port);
        new Thread(() -> {
            try {
                client.connect();
                res.receiveResult(client.getFacadeProxy(),null);
            } catch (IOException e) {
                e.printStackTrace();
                res.receiveResult(null, e);
            }
        }, "connect "+client.getDescriptionString()).start();
        return res;
    }

    Class<? extends Actor> actorClazz;
    T facadeProxy;
    BackOffStrategy backOffStrategy = new BackOffStrategy();

    String host;
    int port;
    ActorClient client;
    int maxTrialConnect = 60; // number of trials on initial connect (each second)
    volatile boolean connected = false;

    public TCPActorClient(Class<? extends Actor> clz, String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        actorClazz = clz;
        facadeProxy = Actors.AsActor( actorClazz, new RemoteScheduler() );
        facadeProxy.__remoteId = 1;
        registerRemoteRefDirect(facadeProxy);
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    public int getMaxTrialConnect() {
        return maxTrialConnect;
    }

    public void setMaxTrialConnect(int maxTrialConnect) {
        this.maxTrialConnect = maxTrialConnect;
    }

    public void connect() throws IOException {
        int count = 0;
        while (count < maxTrialConnect && ! connected ) {
            try {
                client = new ActorClient();
                connected = true;
                facadeProxy.__addRemoteConnection(client);
            } catch (Exception ex) {
                count++;
                Log.Info(this,"connection to " + getDescriptionString() + " failed, retry " + count + " of " + maxTrialConnect);
                if ( count >= maxTrialConnect ) {
                    Log.Lg.error(this,ex,"connection failed. giving up");
                    throw ex;
                }
            }
        }
    }

    private String getDescriptionString() {
        return actorClazz.getSimpleName() + "@" + host + ":" + port;
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
                    currentChannel.set(chan);
                    try {
                        sendLoop(chan);
                    } catch (IOException e) {
                        if (e instanceof SocketException)
                            System.out.println(e);
                        else
                            e.printStackTrace();
                    }
                },
                "sender"
            ).start();
            new Thread(
                () -> {
                    currentChannel.set(chan);
                    receiveLoop(chan);
                },
                "receiver"
            ).start();
        }

        public void close() {
            try {
                chan.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

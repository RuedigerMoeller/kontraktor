package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.*;
import java.net.Socket;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorClient<T extends Actor> extends RemoteRefRegistry {

    public static int BUFFER_SIZE = 64000;

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
            } catch (Exception ex) {
                count++;
                System.out.println("connection to " + getDescriptionString() + " failed, retry " + count + " of " + maxTrialConnect);
                if ( count >= maxTrialConnect ) {
                    System.out.println("connection failed. giving up");
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
    public class ActorClient {

        Socket clientSocket;
        OutputStream outputStream;
        InputStream inputStream;

        public ActorClient() throws IOException {
            clientSocket = new Socket(host, port);
            outputStream = new BufferedOutputStream(clientSocket.getOutputStream(), BUFFER_SIZE);
            inputStream  = new BufferedInputStream(clientSocket.getInputStream(), BUFFER_SIZE);
            ObjectSocket chan = new TCPObjectSocket(inputStream,outputStream,clientSocket,conf);
            new Thread(
                () -> {
                    currentChannel.set(chan);
                    try {
                        sendLoop(chan);
                    } catch (IOException e) {
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

        public void close() throws IOException {
            clientSocket.close();
        }
    }

}

package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.util.function.Consumer;

/**
 * Created by ruedi on 08.08.14.
 *
 * Client side of a tcp actor server.
 * actor refs/callbacks/futures handed out to the actors' server facade are automatically transformed
 * and rerouted, so remoting is mostly transparent.
 */
public class TCPActorClient<T extends Actor> extends ActorClient<T> {

    public static <AC extends Actor> IPromise<AC> Connect( Class<AC> clz, String host, int port ) throws Exception {
        return Connect(clz,host,port,null);
    }

    public static <AC extends Actor> IPromise<AC> Connect( Class<AC> clz, String host, int port, Consumer<Actor> disconnectHandler ) throws Exception {
        if ( disconnectHandler != null ) {
            disconnectHandler = Actors.InThread(disconnectHandler);
        }
        Promise<AC> res = new Promise<>();
        TCPActorClient<AC> client = new TCPActorClient<>( clz, host, port);
        if ( disconnectHandler != null ) {
            client.setDisconnectHandler(disconnectHandler);
        }
        new Thread(() -> {
            try {
                client.connect();
                res.complete(client.getFacadeProxy(), null);
            } catch (IOException e) {
                Log.Info(TCPActorClient.class,null,""+e);
                res.complete(null, e);
            }
        }, "connection thread "+client.getDescriptionString()).start();
        return res;
    }

    String host;
    int port;

    public TCPActorClient(Class<T> clz, String host, int port) throws IOException {
        super(clz);
        this.host = host;
        this.port = port;
    }

    @Override
    protected ObjectSocket createObjectSocket() {
        try {
            return new TCPSocket(host,port,conf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getDescriptionString() {
        return super.getDescriptionString() + "@" + host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}

package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;
import org.nustaq.kontraktor.util.Log;

import javax.management.relation.RoleUnresolved;
import java.io.*;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Created by ruedi on 08.08.14.
 *
 * Client side of a tcp actor server.
 * actor refs/callbacks/futures handed out to the actors' server facade are automatically transformed
 * and rerouted, so remoting is mostly transparent.
 */
public class TCPActorClient<T extends Actor> extends ActorClient<T> {

    public static <AC extends Actor> Future<AC> Connect( Class<AC> clz, String host, int port ) throws Exception {
        return Connect(clz,host,port,null);
    }

    public static <AC extends Actor> AC ConnectSync( Class<AC> clz, String host, int port ) throws Exception
    {
        return ConnectSync(clz,host,port,null);
    }

    /**
     * do a "synchronous" connect (blocks on non-actor thread, awaits nonblocking when called from actor).
     *
     * @return an actor ref or nuöö
     * @throws Exception
     */
    public static <AC extends Actor> AC ConnectSync( Class<AC> clz, String host, int port, Consumer<Actor> disconnectHandler ) throws Exception
    {
        try {
            return Connect(clz, host, port, disconnectHandler).await();
        } catch (Throwable throwable) {
            if ( throwable instanceof Exception )
                throw (Exception) throwable;
            throw new RuntimeException(throwable);
        }
    }

    public static <AC extends Actor> Future<AC> Connect( Class<AC> clz, String host, int port, Consumer<Actor> disconnectHandler ) throws Exception {
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
                res.settle(client.getFacadeProxy(), null);
            } catch (IOException e) {
                Log.Info(TCPActorClient.class,null,""+e);
                res.settle(null, e);
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

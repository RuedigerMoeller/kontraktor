package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by ruedi on 08.08.14.
 *
 * Publish an actor via TCP. actor refs/callbacks/futures handed out to clients are automatically transformed
 * and rerouted, so remoting is mostly transparent.
 *
 * Currently old school blocking IO is used. Should be replaced by a NIO implementation to improve scaling.
 * For a moderate number of clients < ~200 blocking IO is not a problem. Depending on load expect significant performance
 * degradation starting with ~500 clients.
 */
public class TCPActorServer extends ActorServer {


    public static TCPActorServer Publish(Actor act, int port ) throws Exception {
        return Publish(act,port,null);
    }

    public static TCPActorServer Publish(Actor act, int port, Consumer<Actor> closeListener ) throws Exception {
        TCPActorServer server = new TCPActorServer(act, port);
        Promise success = new Promise();
        AtomicReference<Object> res = new AtomicReference<>(null);
        new Thread( ()-> {
            try {
                server.closeListener = closeListener;
                server.acceptLoop();
            } catch (Exception e) {
                if ( !success.isSettled() )
                    success.complete(null, e);
                res.set(e);
            }
        }, "acceptor "+port ).start();
        CountDownLatch latch = new CountDownLatch(1); // bad style, but won't change api now
        success.then( (r,e) -> {
            if ( e != null )
                res.set(e);
             latch.countDown();
        });
        try {
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ( res.get() instanceof Exception )
            throw (Exception) res.get();
        return server;
    }

    int port;
    ServerSocket welcomeSocket;

    public TCPActorServer(Actor proxy, int port) throws Exception {
        super(proxy);
        this.port = port;
    }

    @Override
    protected void connectServerSocket() throws Exception {
        welcomeSocket = new ServerSocket(port);
        Log.Info(this, facade.getActor().getClass().getName() + " running on " + welcomeSocket.getLocalPort());
    }

    @Override
    protected ActorServerConnection acceptConnections() throws Exception {
        Socket connectionSocket = welcomeSocket.accept();
        ActorServerConnection res = new ActorServerConnection();
        TCPSocket tcpSocket = new TCPSocket(connectionSocket, res.getConf());
        res.init(tcpSocket, facade);
        return res;
    }


    @Override
    protected void closeSocket() {
        try {
            welcomeSocket.close();
        } catch (IOException e) {
            Log.Warn(this, e + "");
        }
    }

}

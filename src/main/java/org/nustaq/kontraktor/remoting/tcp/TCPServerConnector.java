package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ActorServerConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.net.TCPObjectSocket;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 *
 * Publishes an actor as a server via blocking TCP. Requires one thread for each client connecting.
 *
 */
public class TCPServerConnector implements ActorServerConnector {

    public static Promise<ActorServer> Publish(Actor facade, int port, Coding coding) {
        Promise finished = new Promise();
        try {
            ActorServer publisher = new ActorServer(new TCPServerConnector(port), facade, coding);
            facade.execute(() -> {
                try {
                    publisher.start();
                    finished.resolve(publisher);
                } catch (Exception e) {
                    finished.reject(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise(null,e);
        }
        return finished;
    }

    int port;
    protected ServerSocket acceptSocket;

    public TCPServerConnector(int port) {
        super();
        this.port = port;
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        Promise p = new Promise();
        new Thread( () -> acceptLoop(facade,port,factory,p), "acceptor thread "+port ).start();
        p.await();
    }

    protected Promise acceptLoop(Actor facade, int port,Function<ObjectSocket, ObjectSink> factory,Promise p) {
        try {
            acceptSocket = new ServerSocket(port);
            p.resolve();
            while (!acceptSocket.isClosed()) {
                Socket clientSocket = acceptSocket.accept();
                MyTCPSocket objectSocket = new MyTCPSocket(clientSocket);
                facade.execute(() -> {
                    ObjectSink sink = factory.apply(objectSocket);
                    new Thread(() -> {
                        while (!clientSocket.isClosed()) {
                            try {
                                Object o = objectSocket.readObject();
                                sink.receiveObject(o, null);
                            } catch (Exception e) {
                                if (e instanceof EOFException == false)
                                    Log.Warn(this, e);
                            }
                        }
                        sink.sinkClosed();
                    }, "tcp receiver").start();
                });
            }
        } catch (Exception e) {
            Log.Info(this, e.getMessage() );
            if ( ! p.isSettled() )
                p.reject(e);
        } finally {
            if ( ! p.isSettled() )
                p.reject("conneciton failed");
            try {
                acceptSocket.close();
            } catch (IOException e) {
                Log.Warn(this,e);
            }
        }
        return p;
    }

    @Override
    public IPromise closeServer() {
        try {
            acceptSocket.close();
        } catch (IOException e) {
            return new Promise<>(null,e);
        }
        return new Promise<>(null);
    }

    static class MyTCPSocket extends TCPObjectSocket implements ObjectSocket {

        public MyTCPSocket(Socket socket) throws IOException {
            super(socket,null);
        }

    }
}

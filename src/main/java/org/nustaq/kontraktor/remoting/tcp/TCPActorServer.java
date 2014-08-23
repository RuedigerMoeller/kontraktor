package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 08.08.14.
 *
 * Can be used to publish a single actor (facade) via TCP. The facade actor then can be used to
 * publish other actor references to remote clients.
 *
 * e.g.
 *     StatefulServer act = Actors.AsActor(StatefulServer.class);
 *     act.$init(4);
 *     TCPActorServer.Publish(act, 6666);
 */
public class TCPActorServer {

    protected List<ActorServerClientConnection> connections = new ArrayList<>();

    public static TCPActorServer Publish(Actor act, int port) throws IOException {
        TCPActorServer server = new TCPActorServer((ActorProxy) act, port);
        new Thread( ()-> {
            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "acceptor "+port ).start();
        return server;
    }

    Actor facadeActor;
    int port;
    ServerSocket welcomeSocket;
    protected volatile boolean terminated = false;

    public TCPActorServer(ActorProxy proxy, int port) throws IOException {
        this.port = port;
        this.facadeActor = (Actor) proxy;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
        connections.forEach( (con) -> con.setTerminated(true) );
    }

    /**
     * warning: consumes calling thread !!
     * @throws IOException
     */
    public void start() throws IOException {
        try {
            welcomeSocket = new ServerSocket(port);
            System.out.println(facadeActor.getActor().getClass().getName() + " running on " + welcomeSocket.getLocalPort());
            while (!terminated) {
                Socket connectionSocket = welcomeSocket.accept();
                ActorServerClientConnection clientConnection = new ActorServerClientConnection(connectionSocket, facadeActor);
                connections.add(clientConnection);
                clientConnection.start();
            }
        } finally {
            setTerminated(true);
        }
    }

    public class ActorServerClientConnection extends RemoteRefRegistry {
        TCPSocket channel;
        Actor facade;

        public ActorServerClientConnection(Socket s, Actor facade) throws IOException {
            super();
            this.channel = new TCPSocket(s,conf);
            this.facade = facade;
        }

        public void start() {
            publishActor(facade); // so facade is always 1
            new Thread(() -> {
                try {
                    currentChannel.set(channel);
                    receiveLoop(channel);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                setTerminated(true);
                connections.remove(ActorServerClientConnection.this);
            }, "receiver").start();
            new Thread(() -> {
                try {
                    currentChannel.set(channel);
                    sendLoop(channel);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                setTerminated(true);
                connections.remove(ActorServerClientConnection.this);
            }, "sender").start();
        }
    }

}

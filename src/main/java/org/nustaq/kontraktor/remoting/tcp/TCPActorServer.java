package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorServer {

    public static int BUFFER_SIZE = 64000;

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

    public TCPActorServer(ActorProxy proxy, int port) throws IOException {
        this.port = port;
        this.facadeActor = (Actor) proxy;
    }

    /**
     * warning: consumes calling thread !!
     * @throws IOException
     */
    public void start() throws IOException {
        welcomeSocket = new ServerSocket(port);
        System.out.println( facadeActor.getActor().getClass().getName() + " running on "+welcomeSocket.getLocalPort());
        while( true ) {
            Socket connectionSocket = welcomeSocket.accept();
            OutputStream outputStream = new BufferedOutputStream(connectionSocket.getOutputStream(), BUFFER_SIZE);
            InputStream inputStream = new BufferedInputStream(connectionSocket.getInputStream(),BUFFER_SIZE);
            new ActorServerClientConnection(outputStream,inputStream,connectionSocket,facadeActor).start();
        }
    }

    public class ActorServerClientConnection extends RemoteRefRegistry {
        TCPObjectSocket channel;
        Actor facade;

        public ActorServerClientConnection(OutputStream out, InputStream in, Socket s, Actor facade) {
            super();
            this.channel = new TCPObjectSocket(in,out,s,conf);
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
            }, "receiver").start();
            new Thread(() -> {
                try {
                    currentChannel.set(channel);
                    sendLoop(channel);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "sender").start();
        }
    }

}

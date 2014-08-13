package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorServer extends RemoteRefRegistry {

    public static int BUFFER_SIZE = 64000;

    public static TCPActorServer Publish(Actor act, int port) throws IOException {
        TCPActorServer server = new TCPActorServer((ActorProxy) act, port);
        server.start();
        return server;
    }

    Actor facade;

    int port;
    ActorServer server;

    public TCPActorServer(ActorProxy proxy, int port) throws IOException {
        this.port = port;
        this.facade = (Actor) proxy;
        publishActor(facade); // so facade is always 1
    }

    public void start() throws IOException {
        server = new ActorServer();
        server.start();
    }

    public class ActorServer {
        ServerSocket welcomeSocket;

        public ActorServer() throws IOException {
            welcomeSocket = new ServerSocket(port);
            System.out.println( facade.getActor().getClass().getName() + " running on "+welcomeSocket.getLocalPort());
        }

        public void start() throws IOException {
            while( true ) {
                Socket connectionSocket = welcomeSocket.accept();
                OutputStream outputStream = new BufferedOutputStream(connectionSocket.getOutputStream(), BUFFER_SIZE);
                InputStream inputStream = new BufferedInputStream(connectionSocket.getInputStream(),BUFFER_SIZE);
                TCPObjectSocket channel = new TCPObjectSocket(inputStream,outputStream,connectionSocket,conf);
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

    public static void main(String arg[]) throws IOException {
        Publish( Actors.AsActor(ServerTestFacade.class), 7777 );
    }
}

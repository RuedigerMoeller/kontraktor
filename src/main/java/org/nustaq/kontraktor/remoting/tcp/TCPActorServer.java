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
public class TCPActorServer extends RemoteRefRegistry {

    Actor facade;

    int port;
    ActorServer server;

    public TCPActorServer(ActorProxy proxy, int port) throws IOException {
        this.port = port;
        this.facade = (Actor) proxy;
        registerPublishedActor(facade); // so facade is always 1
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
                OutputStream outputStream = new BufferedOutputStream(connectionSocket.getOutputStream(), 64000);
                InputStream inputStream = new BufferedInputStream(connectionSocket.getInputStream(),64000);
//                OutputStream outputStream = new DataOutputStream(connectionSocket.getOutputStream());
//                InputStream inputStream  = new DataInputStream(connectionSocket.getInputStream());
                new Thread(() -> {
                    try {
                        currentOutput.set(outputStream);
                        receiveLoop(inputStream,outputStream);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, "receiver").start();
                new Thread(() -> {
                    try {
                        currentOutput.set(outputStream);
                        sendLoop(outputStream);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, "sender").start();
            }
        }

        private void writeOutput(DataOutputStream outputStream) throws IOException, ClassNotFoundException {
//            for (Iterator<Actor> iterator = getRemoteActors().iterator(); iterator.hasNext(); ) {
//                Actor next = iterator.next();
//
//            }
//            System.out.println("received "+read);
        }

    }

    public static void main(String arg[]) throws IOException {
        ServerTestFacade act = Actors.AsActor(ServerTestFacade.class);
        TCPActorServer server = new TCPActorServer((ActorProxy) act,7777);
        server.start();
    }
}

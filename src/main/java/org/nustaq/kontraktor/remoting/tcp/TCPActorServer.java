package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
                DataOutputStream outputStream = new DataOutputStream(connectionSocket.getOutputStream());
                DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());
                new Thread(() -> {
                    try {
                        receiveLoop(inputStream);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                new Thread(() -> {
                    try {
                        sendLoop(outputStream);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
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

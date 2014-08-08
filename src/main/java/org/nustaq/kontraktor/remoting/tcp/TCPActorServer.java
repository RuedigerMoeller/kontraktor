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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorServer {

    Actor facade;
    ConcurrentHashMap<Integer, Actor> actorMapping = new ConcurrentHashMap<>();
    ConcurrentHashMap<Actor, Integer> actorMappingReverse = new ConcurrentHashMap<>();

    BackOffStrategy backOffStrategy = new BackOffStrategy();
    int port;
    ActorServer server;
    RemoteRefRegistry reg = new RemoteRefRegistry();

    public TCPActorServer(ActorProxy proxy, int port) throws IOException {
        this.port = port;
        this.facade = (Actor) proxy;
        reg.getPublishedActorId(facade); // so facade is always 1
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
                        while (true) {
                            readInput(inputStream);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        }

        private void readInput(DataInputStream inputStream) throws IOException, ClassNotFoundException {
            while( true ) {
                // read object
                RemoteCallEntry read = (RemoteCallEntry) reg.readObjectFromStream(inputStream);
//                System.out.println("received " + read);
                if (read.getQueue() == read.MAILBOX) {
                    Actor targetActor = reg.getPublishedActor(read.getReceiverKey());
                    targetActor.getScheduler().dispatchCall(null, facade,read.getMethod(),read.getArgs());
                } else if (read.getQueue() == read.CBQ) {
                    int count = 0;
                    while (!facade.__cbQueue.offer(read)) {
                        backOffStrategy.yield(count++);
                    }
                }
            }

        }

        private void writeOutput(DataInputStream inputStream) throws IOException, ClassNotFoundException {
            // read object
            RemoteCallEntry read = (RemoteCallEntry) reg.readObjectFromStream(inputStream);
            if ( read.getQueue() == read.MAILBOX ) {

            }
            System.out.println("received "+read);
        }

    }

    public static void main(String arg[]) throws IOException {
        ServerTestFacade act = Actors.AsActor(ServerTestFacade.class);
        TCPActorServer server = new TCPActorServer((ActorProxy) act,7777);
        server.start();
    }
}

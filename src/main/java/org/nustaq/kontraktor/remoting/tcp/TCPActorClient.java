package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorClient extends RemoteRefRegistry {

    Actor facadeProxy;
    BackOffStrategy backOffStrategy = new BackOffStrategy();

    String host;
    int port;
    ActorClient client;

    public TCPActorClient(ActorProxy proxy, String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.facadeProxy = (Actor) proxy;
        registerRemoteRefDirect(facadeProxy);
        connect();
    }

    void connect() throws IOException {
        client = new ActorClient();
    }

    /**
     * FIXME: slowish (for a starting ..)
     */
    public class ActorClient {

        Socket clientSocket;
        DataOutputStream outputStream;
        DataInputStream inputStream;

        public ActorClient() throws IOException {
            clientSocket = new Socket(host, port);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            inputStream  = new DataInputStream(clientSocket.getInputStream());
            new Thread(
                () -> {
                    sendLoop(outputStream);
                },
                "sender"
            ).start();
            new Thread(
                () -> {
                    receiveLoop(inputStream);
                },
                "sender"
            ).start();
        }

        public void close() throws IOException {
            clientSocket.close();
        }
    }


    public static void main( String arg[] ) throws IOException, InterruptedException {
        ServerTestFacade test = Actors.AsActor(ServerTestFacade.class, new RemoteScheduler());
        test.__remoteId = 1; // facade is always 1

        ClientSideActor csa = Actors.AsActor(ClientSideActor.class);

        TCPActorClient client = new TCPActorClient((ActorProxy) test,"localhost",7777);
        while( true ) {
            test.$testCall("Hello", csa);
            Thread.sleep(1000);
        }
    }
}

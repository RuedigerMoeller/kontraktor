package uttest;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.serialization.FSTConfiguration;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.Future;

/**
 * Created by ruedi on 28/03/15.
 */
public class WSClient {

    public static void main0(String[] args) {
        String destUri = "ws://localhost:8080/ws";
        if (args.length > 0) {
            destUri = args[0];
        }
        WebSocketClient client = new WebSocketClient();
        SimpleEchoSocket socket = new SimpleEchoSocket();
        try {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);
            socket.awaitClose(5000, TimeUnit.SECONDS);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String destUri = "ws://localhost:8080/ws";
        if (args.length > 0) {
            destUri = args[0];
        }
        try {
            org.nustaq.kontraktor.Future<WSServer.TestA> connect = org.nustaq.kontraktor.remoting.websocket.WebSocketClient.Connect(WSServer.TestA.class, "ws://localhost:8080/ws", null);
            connect.await().$init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @WebSocket
    public static class SimpleEchoSocket {

        private final CountDownLatch closeLatch;

        @SuppressWarnings("unused")
        private Session session;

        public SimpleEchoSocket() {
            this.closeLatch = new CountDownLatch(1);
        }

        public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
            return this.closeLatch.await(duration, unit);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
            this.session = null;
            this.closeLatch.countDown();
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
            System.out.printf("Got connect: %s%n", session);
            this.session = session;
            try {
                new Thread( () -> {
                    boolean run = true;
                    while( run) {
                        Future<Void> fut;
                        fut = session.getRemote().sendStringByFuture("Hello");
                        try {
                            fut.get(2, TimeUnit.SECONDS);
                            fut = session.getRemote().sendStringByFuture("Thanks for the conversation.");
                            fut.get(2, TimeUnit.SECONDS);
                            Thread.sleep(1000);
                            RemoteCallEntry callEntry = new RemoteCallEntry( 0, 1, "$init", new Object[0]);
                            session.getRemote().sendBytes(ByteBuffer.wrap(FSTConfiguration.getDefaultConfiguration().asByteArray(callEntry)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    session.close(StatusCode.NORMAL, "I'm done");
                }).start();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            System.out.printf("Got msg: %s%n", msg);
        }
    }
}

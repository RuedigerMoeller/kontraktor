package bug;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

@ClientEndpoint
public class WSClient {
    public WSClient(URI endpointURI, ObjectSink sink) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {

    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        Log.Info(this, "Connection closed " + reason);
    }

    @OnError
    public void onError( Throwable th ) {
        Log.Warn(this,th);
    }

    AtomicInteger count = new AtomicInteger();
    @OnMessage
    public void onMessage(byte[] message) {
        count.addAndGet(1);
    }

    @OnMessage
    public void onTextMessage(String message) {
    }

    public void sendText(String message) {
    }

    public void sendBinary(byte[] message) {
    }

    public void close() throws IOException {
    }


}

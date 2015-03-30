package uttest;

import io.undertow.Handlers;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServer;
import org.nustaq.kontraktor.undertow.Knode;
import org.nustaq.kontraktor.undertow.websockets.KUndertowWebSocketHandler;

/**
 * Created by ruedi on 29/03/15.
 */
public class WSServer {

    public static void main(String a[]) {
        DispatcherThread.DUMP_CATCHED = true;
        try {
            Knode knode = new Knode();
            knode.mainStub(a);
            WebSocketActorServer webSocketEndpoint = new WebSocketActorServer(new Coding(SerializerType.FSTSer), (ActorProxy)Actors.AsActor(WSTestActor.class));
            knode.getPathHandler().addExactPath("/ws",
                Handlers.websocket(
                    KUndertowWebSocketHandler.Connect(webSocketEndpoint)
                )
            );
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(-1);
        }
    }

}

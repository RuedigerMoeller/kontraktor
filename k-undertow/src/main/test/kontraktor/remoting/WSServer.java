package kontraktor.remoting;

import io.undertow.Handlers;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
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

    public static WebSocketActorServer run() {
        DispatcherThread.DUMP_CATCHED = true;
        try {
            Knode knode = new Knode();
            knode.mainStub(new String[0]);
            WebSocketActorServer webSocketActorServer = new WebSocketActorServer(new Coding(SerializerType.FSTSer), (ActorProxy)Actors.AsActor(ServerTestFacade.class));
            knode.getPathHandler().addExactPath("/ws",
                Handlers.websocket(
                    KUndertowWebSocketHandler.Connect(webSocketActorServer)
                )
            );
            return webSocketActorServer;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

}

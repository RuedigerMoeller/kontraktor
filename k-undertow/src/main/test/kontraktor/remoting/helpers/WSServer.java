package kontraktor.remoting.helpers;

import io.undertow.Handlers;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.undertow.Knode;
import org.nustaq.kontraktor.undertow.KUndertowHttpServerAdapter;
import org.nustaq.kontraktor.undertow.websockets.KUndertowWebSocketHandler;

/**
 * Created by ruedi on 29/03/15.
 */
public class WSServer {

    public static WebSocketActorServerAdapter run() {
        Class<ServerTestFacade> actorClazz = ServerTestFacade.class;
        return getWebSocketActorServer(Actors.AsActor(actorClazz),8080);
    }

    public static WebSocketActorServerAdapter getWebSocketActorServer(Actor actor,int port) {
        DispatcherThread.DUMP_CATCHED = true;
        try {
            Knode knode = new Knode();
            knode.mainStub(new String[] {"-p",""+port});
            WebSocketActorServerAdapter webSocketActorServer
                = new WebSocketActorServerAdapter(
                    new KUndertowHttpServerAdapter(knode.getServer(),knode.getPathHandler()),
                    new Coding(SerializerType.FSTSer),
                    actor, false
            );
            knode.getPathHandler().addExactPath("/ws",
                Handlers.websocket(
                    KUndertowWebSocketHandler.With(webSocketActorServer).cb
                )
            );
            return webSocketActorServer;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return null;
    }

}

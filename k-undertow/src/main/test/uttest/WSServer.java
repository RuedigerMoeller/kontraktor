package uttest;

import io.undertow.Handlers;
import io.undertow.Undertow;
import org.nustaq.kontraktor.remoting.http.websocket.WebSocketEndpoint;
import org.nustaq.kontraktor.undertow.Knode;
import org.nustaq.kontraktor.undertow.websockets.KWebSocketHandler;

/**
 * Created by ruedi on 29/03/15.
 */
public class WSServer {

    public static void main(String a[]) {
        try {
            Knode knode = new Knode();
            knode.mainStub(a);
            knode.getPathHandler().addExactPath("/ws",
                Handlers.websocket(
                    KWebSocketHandler.Connect(new WebSocketEndpoint(null,1,1)) ));
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(-1);
        }
    }

}

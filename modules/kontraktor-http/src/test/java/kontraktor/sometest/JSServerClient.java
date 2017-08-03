package kontraktor.sometest;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.babel.BabelOpts;
import org.nustaq.kontraktor.babel.BabelResult;
import org.nustaq.kontraktor.babel.BrowseriBabelify;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

public class JSServerClient extends Actor {

    // dummy prototype, impl is javascript
    public void withCallback(String s, Callback cb ) {}

    public static void main(String[] args) {
        WebSocketConnectable webSocketConnectable =
            new WebSocketConnectable(BrowseriBabelify.class, "ws://localhost:3998/")
                .serType(SerializerType.JsonNoRef)
                .actorClass(JSServerClient.class);
        JSServerClient remote = (JSServerClient) webSocketConnectable.connect().await();

        remote.withCallback("java", (r,e) -> System.out.println(r+" "+e));
    }
}

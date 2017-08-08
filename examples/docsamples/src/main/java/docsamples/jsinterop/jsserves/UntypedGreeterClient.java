package docsamples.jsinterop.jsserves;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

public class UntypedGreeterClient {

    // NOTE: due to a temporary bug in 4.03, untyped java clients DO NOT WORK
    // with Java-implemented Servers. Use typed clients meanwhile
    public static void main(String[] args) {
        Actor remote = (Actor) new WebSocketConnectable()
            .actorClass(Actor.class)
            .url("ws://localhost:3999")
            .serType(SerializerType.JsonNoRef)
            .connect((x, y) -> System.out.println("disconnect " + x + " " + y)).await();

        remote.ask("greet","Kontraktor", 1000 )
            .then((r, e) -> System.out.println(r));
    }
}

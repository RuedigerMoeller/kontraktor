package docsamples.jsinterop.javaserves;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

public class PlainGreeterClient {
    public static void main(String[] args) {
        Greeter remote = (Greeter) new WebSocketConnectable()
            .actorClass(Greeter.class)
            .url("ws://localhost:3999")
            .serType(SerializerType.JsonNoRef)
            .connect( (x,y) -> System.out.println("disconnect " + x+" "+y)).await();

        remote.greet("World", 1000)
            .then( r -> System.out.println(r) );
    }

}

package docsamples.jsinterop.jsserves.typedclient;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

// connect to a javascript service using a dummy actor providing types
public class TypedGreeterClient {

    public static void main(String[] args) {
        IGreeter remote = (IGreeter) new WebSocketConnectable()
            .actorClass(IGreeter.class)
            .url("ws://localhost:3999")
            .serType(SerializerType.JsonNoRef)
            .connect( (x,y) -> System.out.println("disconnect " + x+" "+y)).await();

        remote.greet("World", 1000)
            .then( r -> System.out.println(r) );
    }

}

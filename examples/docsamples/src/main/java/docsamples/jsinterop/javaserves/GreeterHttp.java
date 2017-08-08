package docsamples.jsinterop.javaserves;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;

import static org.nustaq.kontraktor.Actors.AsActor;

/**
 * uses http publisher instead of websocket
 */
public class GreeterHttp {

    public static void main(String[] args) {
        new HttpPublisher(AsActor(Greeter.class),"localhost", "/test", 8888)
            .serType(SerializerType.JsonNoRef)
            .publish( x -> System.out.println("DISCONNECTED:"+x));
    }

}

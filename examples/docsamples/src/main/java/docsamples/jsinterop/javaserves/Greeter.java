package docsamples.jsinterop.javaserves;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Greeter extends Actor<Greeter> {

    public IPromise<String> greet(String name, long duration ) {
        Promise res = new Promise();
        delayed(duration,() -> res.resolve("Hello "+name));
        return res;
    }

    public static void main(String[] args) {
        new WebSocketPublisher()
            .facade(AsActor(Greeter.class))
            .serType(SerializerType.JsonNoRef)
            .urlPath("/")
            .port(3999)
            .publish();
    }
}

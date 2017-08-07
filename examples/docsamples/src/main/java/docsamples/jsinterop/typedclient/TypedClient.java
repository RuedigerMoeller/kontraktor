package docsamples.jsinterop.typedclient;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

/**
 * ISampleService provides typed versions of SampleService.js
 *
 * so its possible to talk to the javascript remote service in a
 * typesafe fashion.
 *
 */
public class TypedClient {
    public static void main(String[] args) {
        JSR356ClientConnector.DumpProtocol = true;
        ISampleService remote = (ISampleService) new WebSocketConnectable()
            .actorClass(ISampleService.class)
            .url("ws://localhost:3998")
            .serType(SerializerType.JsonNoRef)
            .connect( (x,y) -> System.out.println("disconnect " + x+" "+y)).await();

        remote.withPromise("Hello")
            .then( (r,e) -> System.out.println(r+" "+e));
        remote.withCallback(
            "Hi",
            (r, e) -> System.out.println("callback:"+r+" "+e)
        );
        remote.withCallbackAndPromise(
            "Hi CB+P ",
            (r, e) -> System.out.println("callback:"+r+" "+e)
        ).then( (r,e) -> System.out.println(r+" "+e));

        remote.getSingletonSubserviceTyped("lol").then( (sub,err) -> {
           sub.withCallbackAndPromise("from java", (r,e) -> System.out.println(r+" "+e))
            .then( (r,e) -> System.out.println("withCallbackAndPromise prom:"+r+" "+e));
        });
    }
}

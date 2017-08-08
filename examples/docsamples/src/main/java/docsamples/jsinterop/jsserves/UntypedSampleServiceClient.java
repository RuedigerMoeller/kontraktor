package docsamples.jsinterop.jsserves;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

/**
 * assumes SampleService.js is running
 *
 * Shows how to talk "untyped" to a remote javascript service
 * see TypedSampleServiceClient for a typesafe variant.
 */
public class UntypedSampleServiceClient {
    public static void main(String[] args) {
        Actor remote = (Actor)new WebSocketConnectable()
            .actorClass(Actor.class)
            .url("ws://localhost:3998")
            .serType(SerializerType.JsonNoRef)
            .connect( (x,y) -> System.out.println("disconnect " + x+" "+y)).await();

        remote.ask("withPromise",
            "Hello"
        ).then( (r,e) -> System.out.println(r+" "+e));
        remote.tell("withCallback",
            "Hi",
            (Callback) (r, e) -> System.out.println("callback:"+r+" "+e)
        );
        remote.ask("withCallbackAndPromise",
            "Hi CB+P ",
            (Callback) (r, e) -> System.out.println("callback:"+r+" "+e)
        ).then( (r,e) -> System.out.println(r+" "+e));

        remote.ask("getSingletonSubservice","lol").
            then( (subremote,err) -> {
                Actor subrem = (Actor) subremote;
                subrem.tell("voidFun");
                subrem.ask("withCallbackAndPromise",
                    "Hi sub",
                    (Callback) (r,e) -> System.out.println("from sub"+r+" "+e) );
            });
    }
}

package knode;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;

/**
 * Created by ruedi on 01/08/15.
 */
public class Service4Node extends Actor<Service4Node> {

    public void hello(String who) {
        System.out.println("hello "+who);
    }

    public IPromise concat( int a, int b, String separator ) {
        System.out.println("concat "+a+" "+b+" "+separator);
        return new Promise<>(a+separator+b);
    }

    public static void main(String[] args) {

        Service4Node service = Actors.AsActor(Service4Node.class);
        new WebSocketPublisher(service,"localhost","/s4n",8080)
            .serType(SerializerType.JsonNoRef)
            .publish(actor -> {
                System.exit(0);
            });

    }

}

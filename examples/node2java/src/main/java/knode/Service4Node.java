package knode;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;

import java.util.HashMap;
import java.util.Map;

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

    public IPromise typeTest( int i, double f, double d, String sa[], int ia[], double da[] ) {
        return new Promise<>(new Object[] {
            i,f,d,sa,ia,da
        });
    }

    public IPromise<Map> mapOut() {
        HashMap mp = new HashMap();
        mp.put("X",234);
        mp.put(123,new double[]{2113.23,123234.234});
        mp.put(123.23,new int[] {1,2,3,-555});
        return resolve(mp);
    }

    public IPromise<Map> mapIn(Map mp) {
        return resolve(mp);
    }

    public static void main(String[] args) {

        Service4Node service = Actors.AsActor(Service4Node.class);
        new WebSocketPublisher(service,"localhost","/s4n",8080)
            .serType(SerializerType.JsonNoRef)
            .sendStringMessages(true)
            .publish(actor -> {
                System.exit(0);
            });

    }

}

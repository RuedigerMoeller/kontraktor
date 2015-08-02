package knode;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.coders.Unknown;

import java.util.Arrays;
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
        System.out.println("mapout");
        HashMap mp = new HashMap();
        mp.put("X",234);
        mp.put(123,new double[]{2113.23,123234.234});
        mp.put(123.23, new int[]{1, 2, 3, -555});
        return resolve(mp);
    }

    public IPromise<Map> mapIn(Map mp) {
        System.out.println("mapin");
        return resolve(mp);
    }

    public IPromise<Pojo2Node> pojoOut() {
        System.out.println("pojoout");
        return resolve(new Pojo2Node().initVals());
    }

    public IPromise<Pojo2Node> pojoIn(Pojo2Node pj) {
        System.out.println("pojoin");
        return resolve(pj);
    }

    public void receiveUnknown( Unknown unknown ) {
        System.out.println("unknown"+unknown);
        System.out.println("x:"+unknown.getInt("x"));
        System.out.println("y:"+unknown.getDouble("y"));
        System.out.println("z:"+unknown.getString("z"));
        System.out.println("xx:" + Arrays.toString(unknown.getArr("xx")));
    }

    public static void main(String[] args) {
        Log.setLevel( Log.DEBUG ); // ensure we see also IO/close exceptions

        Service4Node service = Actors.AsActor(Service4Node.class);
        new WebSocketPublisher(service,"localhost","/s4n",8080)
            //.serType(SerializerType.JsonNoRef) gets replaced by this below
            .coding(new Coding(SerializerType.JsonNoRef, Pojo2Node.class))
            .sendStringMessages(true)
            .publish(actor -> {
                System.exit(0);
            });

    }

}

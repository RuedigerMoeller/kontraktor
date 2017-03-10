package wapi;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.server.WapiServer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 10.03.17.
 */
public class DummyClient {
    public static void main(String[] args) throws InterruptedException {
        HttpConnectable con = new HttpConnectable(WapiServer.class,"http://localhost:7777/api").coding(new Coding(SerializerType.JsonNoRef));
        WapiServer connect = (WapiServer) con.connect((x, y) -> System.out.println("" + x + y)).await();
        Actor dummyService = (Actor) connect.getService("DummyService", "1").await();
        dummyService.ask("service","hello").then( (x,y) -> System.out.println(x));

//        DummyService dummyService1 = (DummyService) connect.getService("DummyService", "1").await();
//        dummyService1.service("hello1").then( (x,y) -> System.out.println(x));

//        while( true ) {
//            AtomicLong sum = new AtomicLong();
//            for ( int i = 0; i < 1000; i++ ) {
//                dummyService.ask("roundTrip", System.currentTimeMillis()).then( l -> sum.addAndGet(System.currentTimeMillis()-(Long)l));
//                Thread.yield();
//            }
//            Thread.sleep(1000L);
//            System.out.println("lat "+sum.get()/1000);
//        }
    }
}

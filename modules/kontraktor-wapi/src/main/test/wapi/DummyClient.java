package wapi;

import org.nustaq.kontraktor.barebone.Callback;
import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.kontraktor.barebone.RemoteActorConnection;

/**
 * Created by ruedi on 10.03.17.
 */
public class DummyClient {
    public static void main(String[] args) throws InterruptedException {
        final RemoteActorConnection act = new RemoteActorConnection( s -> System.out.println("connection closed:"+s) );
        final RemoteActor facade = act.connect("http://localhost:7777/dummyservice", true).await();
        System.out.println("facade:" + facade);
        facade.ask("service", "hello" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("foreign", new ForeignClass(1,2,3) ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("service", "holla" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.tell("subscribe", new ForeignClass(1,2,3),
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
//        facade.tell("subscribe",
//            new Callback() {
//                @Override
//                public void receive(Object result, Object error) {
//                    System.out.println(result);
//                }
//            }
//        );

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

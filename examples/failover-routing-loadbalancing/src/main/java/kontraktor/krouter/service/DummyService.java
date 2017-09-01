package kontraktor.krouter.service;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.routers.SimpleKrouter;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;

/**
 * Created by ruedi on 09.03.17.
 */
public class DummyService extends Actor implements RemotedActor {

    public void init() {
    }

    public IPromise<DummySubService> createSubService() {
        DummySubService dummySubService = AsActor(DummySubService.class, getScheduler());
        return new Promise<>(dummySubService);
    }

    public void subscribe(ForeignClass in, Callback dummy) {
        System.out.println("subscribe "+dummy);
        pingIt(10,in, dummy);
    }

    private void pingIt(int i, ForeignClass in, Callback dummy) {
        if ( i < 0 ) {
            dummy.finish();
            return;
        }
        System.out.println("pinging "+i);
        in.x = i;
        dummy.pipe(in );
        delayed( 100, () -> pingIt(i-1, in, dummy) );
    }

    RateMeasure rtMeasure = new RateMeasure("roundtrip").print(true);
    public IPromise roundTrip(long cur) {
        rtMeasure.count();
        return resolve(cur);
    }

    int count = 0;
    RateMeasure simpleMeasure = new RateMeasure("simple").print(true);
    public void simpleBench(int something) {
        count++;
        simpleMeasure.count();
    }
    public void resetSimpleBench() {
        count = 0;
    }

    public static void main(String[] args) {
        DummyService serv = Actors.AsActor(DummyService.class);
        serv.init();

        serv.execute( () -> {
            SimpleKrouter krouter = (SimpleKrouter)
//                new WebSocketConnectable()
//                    .url("ws://localhost:8888/binary")
//                    .actorClass(SimpleKrouter.class)
//                    .serType(SerializerType.FSTSer)
//                    .connect( (x,err) -> {
//                        System.out.println("discon "+x);
//                        System.exit(-1);
//                    }).await();
            new TCPConnectable()
                .host("localhost").port(6667)
                .actorClass(SimpleKrouter.class)
                .serType(SerializerType.FSTSer)
                .connect( (x,err) -> {
                    System.out.println("discon "+x);
                    System.exit(-1);
                }).await();

            krouter.router$Register(serv.getUntypedRef()).await();
            Log.Info(DummyService.class,"service registered at krouter");
//        new TCPNIOPublisher().port(6666).serType(SerializerType.JsonNoRef).facade(serv).publish();
//            new WebSocketPublisher().hostName("localhost").urlPath("/service").port(6666).serType(SerializerType.JsonNoRef).facade(serv).publish();
        });
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {
        System.out.println("service unpublished "+connectionIdentifier);
    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {
        System.out.println("facade service published");
    }
}

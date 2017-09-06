package kontraktor.krouter.service;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.routers.Krouter;
import org.nustaq.kontraktor.routers.Routing;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;

/**
 * Created by ruedi on 09.03.17.
 */
public class DummyService extends Actor<DummyService> {

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

    public IPromise pingForeign(ForeignClass in) {
        return resolve(in);
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

        Krouter krouter = (Krouter) Routing.registerService(
            new WebSocketConnectable()
                .url("ws://localhost:8888/binary")
                .actorClass(Krouter.class)
                .serType(SerializerType.FSTSer),
            serv,
            x -> {
                System.out.println("discon " + x);
                System.exit(-1);
            }
        ).await();

        Log.Info(DummyService.class,"service registered at krouter");
    }

}

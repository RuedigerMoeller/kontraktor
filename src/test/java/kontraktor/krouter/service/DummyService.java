package kontraktor.krouter.service;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.routers.SimpleKrouter;

/**
 * Created by ruedi on 09.03.17.
 */
public class DummyService extends Actor {

    public void init() {
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
        delayed( 1000, () -> pingIt(i-1, in, dummy) );
    }

    public IPromise roundTrip(long cur) {
        return resolve(cur);
    }

    public static void main(String[] args) {
        // start the service
        DummyService serv = Actors.AsActor(DummyService.class);
        serv.init();

        // publish it on the krouter so clients can access it
        SimpleKrouter krouter = (SimpleKrouter) new TCPConnectable()
            .host("localhost").port(6667)
            .actorClass(SimpleKrouter.class)
            .serType(SerializerType.JsonNoRef)
            .connect( (x,err) -> System.out.println("discon "+x) ).await();
        krouter.router$RegisterService(serv.getUntypedRef()).await();
    }

}

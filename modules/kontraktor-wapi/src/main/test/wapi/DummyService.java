package wapi;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.server.WapiDescription;
import org.nustaq.kontraktor.server.WapiRegistry;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 09.03.17.
 */
public class DummyService extends Actor {

    WapiRegistry wapi;
    WapiDescription dummyService;

    public void init() {
        TCPConnectable con = new TCPConnectable(WapiRegistry.class,"localhost",6665);
        con.coding(new Coding(SerializerType.FSTSer));
        wapi = (WapiRegistry) con.connect((x, err) -> {
            System.out.println("disconnect");
        }).await(15000);

        dummyService = new WapiDescription()
            .name("DummyService");

        wapi.registerService( dummyService, self() );

        Log.Info(this,"connected wapireg");
        heartbeat();
    }

    public IPromise service(String dummy) {
        System.out.println("service "+dummy);
        return resolve(dummy+" "+System.currentTimeMillis());
    }

    public void heartbeat() {
        if ( ! isStopped() ) {
            wapi.receiveHeartbeat(dummyService.getName(),dummyService.getUniqueKey());
            delayed(3000,() -> heartbeat());
        }
    }

    public static void main(String[] args) {
        Actor serv = Actors.AsUntypedActor(new DummyService());
        serv.tell("init");
    }

}
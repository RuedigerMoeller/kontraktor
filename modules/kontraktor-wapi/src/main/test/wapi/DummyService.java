package wapi;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
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
    boolean connected = false;

    public void init() {
        connect();
    }

    public void connect() {
        if ( isStopped() )
            return;
        TCPConnectable con = new TCPConnectable(WapiRegistry.class,"localhost",6665);
        con.coding(new Coding(SerializerType.JsonNoRef));
        con.connect((x, err) -> {
            Log.Warn(this, "disconnected");
            connected = false;
            delayed(1000,() -> connect() );
        }).then( (wapires,error) -> {
            if ( wapires != null && error == null ) {
                wapi = (WapiRegistry)wapires;
                dummyService = new WapiDescription()
                    .name("DummyService");
                wapi.registerService( dummyService, self() );
                Log.Info(this,"connected wapireg");
                connected = true;
                heartbeat();
            } else {
                Log.Warn(this, "disconnected retry in 1 second");
                delayed(1000,() -> connect() );
            }
        });

    }

    public IPromise service(String dummy) {
        System.out.println("service "+dummy);
        return resolve(dummy+" "+System.currentTimeMillis());
    }

    public IPromise<ForeignClass> foreign(ForeignClass in) {
        System.out.println("foreign "+in);
        return resolve(in);
    }

    public void subscribe(ForeignClass in, Callback dummy) {
        System.out.println("subscribe "+dummy);
        pingIt(in, dummy);
    }

    private void pingIt(ForeignClass in, Callback dummy) {
        System.out.println("pinging");
        dummy.stream(in );
        delayed( 1000, () -> pingIt( in, dummy) );
    }

    public IPromise roundTrip(long cur) {
        return resolve(cur);
    }

    public void heartbeat() {
        if ( ! isStopped() && connected ) {
            wapi.receiveHeartbeat(dummyService.getName(),dummyService.getUniqueKey());
            delayed(3000,() -> heartbeat());
        }
    }

    public static void main(String[] args) {
        Actor serv = Actors.AsUntypedActor(new DummyService());
        serv.tell("init");
    }

}

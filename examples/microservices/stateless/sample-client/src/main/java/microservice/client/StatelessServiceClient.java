package microservice.client;

import microservice.pub.IStatelessService;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.Log;

public class StatelessServiceClient extends Actor<StatelessServiceClient> {

    IStatelessService service;

    public IPromise init(ConnectableActor krouter) {
        try {
            service = (IStatelessService) krouter.connect(null, act -> {
                self().didDisconnect(act);
            }).await();

            cyclic(1, () -> simulate());

            return resolve(true);
        } catch (Exception e) {
            return reject(e);
        }
    }

    long lastMsg = System.currentTimeMillis();
    private boolean simulate() {
        service.getTime(1000)
            .then( (r,e) -> {
                if ( System.currentTimeMillis() - lastMsg > 1000 ) {
                    lastMsg = System.currentTimeMillis();
                    if (e != null)
                        System.out.println("err " + e);
                    else {
                        System.out.println("tim " + (System.currentTimeMillis() - (long) r));
                    }
                }
            });
        return !service.isStopped();
    }

    // disconnect from krouter
    public void didDisconnect(Actor act) {
        Log.Info(this,"disconnected "+act);
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            // connect with binary encoding
            StatelessServiceClient cl = AsActor(StatelessServiceClient.class);

            cl.init(
                new WebSocketConnectable()
                    .url("ws://localhost:6667/slservice/v1/bin")
                    .serType(SerializerType.FSTSer)
                    .actorClass(IStatelessService.class)
            ).then((r, e) -> {
                if (e != null) {
                    Log.Error(StatelessServiceClient.class, (Throwable) e);
                }
            });
        } else {
            // connect with json encoding
            StatelessServiceClient cl = AsActor(StatelessServiceClient.class);

            cl.init(
                new WebSocketConnectable()
                    .url("ws://localhost:6667/slservice/v1/json")
                    .serType(SerializerType.JsonNoRef)
                    .actorClass(IStatelessService.class)
            ).then((r, e) -> {
                if (e != null) {
                    Log.Error(StatelessServiceClient.class, (Throwable) e);
                }
            });
        }
    }
}

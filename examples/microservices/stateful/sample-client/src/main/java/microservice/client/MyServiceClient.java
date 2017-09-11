package microservice.client;

import microservice.pub.IMyService;
import microservice.pub.Item;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.Log;

public class MyServiceClient extends Actor<MyServiceClient> {

    IMyService service;

    public IPromise init(ConnectableActor krouter) {
        try {
            service = (IMyService) krouter.connect(null, act -> {
                self().didDisconnect(act);
            }).await();
            service.setServerMsgCallback((r, e) -> self().serverMsgCallback((RemoteCallEntry) r, e));
            registerListener();
            cyclic(1000, () -> simulate());
            return resolve(true);
        } catch (Exception e) {
            return reject(e);
        }
    }

    private void registerListener() {
        service.addChangeListener((r,e) -> {
            Log.Info(this,"change received "+r);
        });
    }

    public void serverMsgCallback(RemoteCallEntry r, Object e) {
        Log.Info(this,"serverMsgCallback "+r+" "+e);
        if ( "krouterTargetDidChange".equals(r.getMethod()) ) {
            registerListener();
        }
    }

    private boolean simulate() {
        service.publishItem(new Item().name("item"+Math.random()).description("a very unique item"))
            .then( (r,e) -> {
                if ( e != null )
                    System.out.println("res "+r+" "+e);
            });
        return true;
    }

    // disconnect from krouter
    public void didDisconnect(Actor act) {
        Log.Info(this,"disconnected "+act);
    }

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ) {
            MyServiceClient cl = AsActor(MyServiceClient.class);
            cl.init(
                new WebSocketConnectable()
                    .url("ws://localhost:6667/myservice/v1/bin")
                    .serType(SerializerType.FSTSer)
                    .actorClass(IMyService.class)
            ).then((r, e) -> {
                if (e != null) {
                    Log.Error(MyServiceClient.class, (Throwable) e);
                }
            });
        } else {
            MyServiceClient cl = AsActor(MyServiceClient.class);
            cl.init(
                new WebSocketConnectable()
                    .url("ws://localhost:6667/myservice/v1/json")
                    .serType(SerializerType.JsonNoRef)
                    .actorClass(IMyService.class)
            ).then((r, e) -> {
                if (e != null) {
                    Log.Error(MyServiceClient.class, (Throwable) e);
                }
            });
        }
    }
}

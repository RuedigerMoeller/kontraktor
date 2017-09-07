package microservice.client;

import microservice.pub.ChangeMessage;
import microservice.pub.IMyService;
import microservice.pub.Item;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.routing.WSKrouterStarterConfig;
import org.nustaq.kontraktor.util.Log;

import java.util.*;

public class MyServiceClient extends Actor<MyServiceClient> {

    IMyService service;

    public IPromise init(ConnectableActor krouter) {
        service = (IMyService) krouter.connect(null, act -> {
            self().didDisconnect(act);
        }).await();
        service.addChangeListener((r,e) -> {
            Log.Info(this,"change received "+r);
        });
        cyclic(1000, () -> simulate() );
        return resolve(true);
    }

    private boolean simulate() {
        service.addItem(new Item().name("item"+Math.random()).description("a very unique item"));
        return true;
    }

    public void didDisconnect(Actor act) {
        Log.Info(this,"disconnected "+act);
    }

    public static void main(String[] args) {
        MyServiceClient cl = AsActor(MyServiceClient.class);
        cl.init(
            new WebSocketConnectable()
                .url("ws://localhost:6667/myservice/v1/bin")
                .serType(SerializerType.FSTSer)
                .actorClass(IMyService.class)
        );
    }
}

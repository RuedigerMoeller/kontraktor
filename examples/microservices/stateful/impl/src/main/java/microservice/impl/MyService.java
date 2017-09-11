package microservice.impl;

import com.beust.jcommander.JCommander;
import microservice.pub.ChangeMessage;
import microservice.pub.IMyService;
import microservice.pub.Item;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.routers.Routing;
import org.nustaq.kontraktor.util.Log;

import java.util.*;

public class MyService extends IMyService<MyService> {

    Map<String,Callback<ChangeMessage>> listeners;

    @Local public void init() {
        listeners = new HashMap<>();
    }

    @Override
    public IPromise publishItem(Item item) {
        fireChange(new ChangeMessage().action(ChangeMessage.Action.ADDED).item(item));
        Log.Info(this,"received pub item");
        return resolve(true);
    }

    private void fireChange(ChangeMessage item) {
        listeners.values().forEach( li -> li.pipe(item) );
    }

    @Override
    public IPromise<String> addChangeListener(Callback<ChangeMessage> listener) {
        String id = UUID.randomUUID().toString();
        listeners.put(id,listener);
        return resolve(id);
    }

    @Override
    public void removeChangeListener(String id) {
        listeners.remove(id);
    }

    public void disconnedtedCB(Actor x) {
    }

    public static void main(String[] args) {
        MyServiceArgs conf = new MyServiceArgs();
        JCommander.newBuilder().addObject(conf).build().parse(args);

        MyService serv = AsActor(MyService.class);
        serv.init();

        conf.connectUrls.forEach( url -> {
            Routing.registerService(
                new WebSocketConnectable()
                    .url(url)
                    .serType(url.endsWith("/bin") ? SerializerType.FSTSer : SerializerType.JsonNoRef ),
                serv,
                x -> serv.disconnedtedCB(x),
                true
            ).then( (r,e) -> {
                if ( e != null ) {
                    Log.Info(MyService.class, "error connecting krouter " + e);
                    if ( e instanceof Throwable )
                        Log.Info(MyService.class, (Throwable) e);
                } else
                    Log.Info(MyService.class, "MyService connected krouter "+url);
            });
        });
    }

}

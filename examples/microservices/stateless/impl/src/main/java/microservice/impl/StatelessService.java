package microservice.impl;

import com.beust.jcommander.JCommander;
import microservice.pub.IStatelessService;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.routers.Routing;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;

public class StatelessService extends IStatelessService<StatelessService> {

    RateMeasure measure = new RateMeasure("calls").print(true);

    @Override
    public IPromise<Long> getTime(long delay) {
        Promise p = promise();
        long now = System.currentTimeMillis();
        delayed(delay, () -> {
            measure.count();
            p.resolve(now);
        });
        return p;
    }

    public void disconnedtedCB(Actor x) {
        Log.Warn(this,"disconnected from krouter");
    }

    public static void main(String[] args) {
        StatelessServiceArgs conf = new StatelessServiceArgs();
        JCommander.newBuilder().addObject(conf).build().parse(args);

        StatelessService serv = AsActor(StatelessService.class);

        conf.connectUrls.forEach( url -> {
            Routing.registerService(
                new WebSocketConnectable()
                    .url(url)
                    .serType(url.endsWith("/bin") ? SerializerType.FSTSer : SerializerType.JsonNoRef ),
                serv,
                x -> serv.disconnedtedCB(x),
                false
            ).then( (r,e) -> {
                if ( e != null ) {
                    Log.Info(StatelessService.class, "error connecting krouter " + e);
                    if ( e instanceof Throwable )
                        Log.Info(StatelessService.class, (Throwable) e);
                } else
                    Log.Info(StatelessService.class, "SLService connected krouter "+url);
            });
        });
    }

}

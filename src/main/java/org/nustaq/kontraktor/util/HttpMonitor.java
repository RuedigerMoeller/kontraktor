package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.monitoring.Monitorable;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;

import java.util.HashMap;

/**
 * Created by ruedi on 18.10.14.
 */
public class HttpMonitor extends Actor<HttpMonitor> {

    public static int DEFAULT_PORT = 7777;

    static HttpMonitor instance;
    public static HttpMonitor getInstance() {
        synchronized (HttpMonitor.class) {
            if (instance == null) {
                instance = RestActorServer.publish( HttpMonitor.class, "monitor", DEFAULT_PORT );
            }
        }
        return instance;
    }

    /**
     * Flattens a monitorable hierarchy to a stream, as http remoting does not support remote refs
     *
     * method name is without '$' as it gets part of url
     */
    public void report( String name, int depth, Callback cb ) {
        if ( depth == 0 ) {
            depth = 1;
        }
        Monitorable monitorable = monitored.get(name);
        if ( monitorable == null )
            cb.receive("no such monitorable registered:"+name, "not found");
        else {
            sendMonitorable(depth,monitorable).then((result, err) -> {
                cb.receive(result, null);
            });
        }
    }

    protected Future<Object[]> sendMonitorable(int depth, Monitorable monitorable) {
//        System.out.println("dumpmon " + monitorable);
        Promise p = new Promise();
        monitorable.$getReport().then( ( report, err ) -> {
            Object result[] = new Object[2];
            result[0] = report;
            monitorable.$getSubMonitorables().then((monitorables, errmon) -> {
                if ( monitorables.length > 0 && depth >= 1 ) {
                    Object[] subResult = new Object[monitorables.length];
                    result[1] = subResult;
                    Future futs[] = new Future[monitorables.length];
                    for (int i = 0; i < monitorables.length; i++) {
                        Monitorable submon = monitorables[i];
                        futs[i] = sendMonitorable(depth-1,submon);
                    }
                    yield(futs).then( (futures,err0) -> {
                        for (int i = 0; i < futures.length; i++) {
                            Future future = futures[i];
                            subResult[i] = future.getResult();
                        }
                        p.receive(result,null);
                    });
                } else
                    p.receive(result,null);
            });
        });
        return p;
    }

    HashMap<String,Monitorable> monitored = new HashMap();
    @Local
    public void $publish(String name, Monitorable toPublish) {
        monitored.put(name, toPublish );
    }

}

package org.nustaq.kontraktor.remoting.http_old;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.monitoring.Monitorable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ruedi on 18.10.14.
 */
public class HttpMonitor extends Actor<HttpMonitor> {

    public static int DEFAULT_PORT = 9999;

    static HttpMonitor instance;
    public static HttpMonitor getInstance() {
        synchronized (HttpMonitor.class) {
            if (instance == null) {
                instance = RestActorServer.Publish(HttpMonitor.class, "monitor", DEFAULT_PORT);
            }
        }
        return instance;
    }

    public IPromise<String[]> getMonitorableKeys(String simpleClzName) {
        ArrayList<String> result = new ArrayList();
        monitored.entrySet().forEach( (entry) -> {
            Monitorable mon = entry.getValue();
            if ( mon instanceof Actor)
                mon = ((Actor)mon).getActor();
            if ( entry.getValue() != null && mon.getClass().getSimpleName().equals(simpleClzName) ) {
                result.add(entry.getKey());
            }
        });
        String res[] = new String[result.size()];
        result.toArray(res);
        return new Promise(res);
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
            cb.settle(null, "no such monitorable registered: '" + name + "'");
        else {
            getMonitorables(depth, monitorable).then((result, err) -> {
                cb.settle(result, null);
            });
        }
    }

    protected IPromise<Object[]> getMonitorables(int depth, Monitorable monitorable) {
//        System.out.println("dumpmon " + monitorable);
        Promise p = new Promise();
        monitorable.$getReport().then( ( report, err ) -> {
            Object result[] = new Object[2];
            result[0] = report;
            monitorable.$getSubMonitorables().then((monitorables, errmon) -> {
                if ( monitorables.length > 0 && depth >= 1 ) {
                    Object[] subResult = new Object[monitorables.length];
                    result[1] = subResult;
                    IPromise futs[] = new IPromise[monitorables.length];
                    for (int i = 0; i < monitorables.length; i++) {
                        Monitorable submon = monitorables[i];
                        futs[i] = getMonitorables(depth - 1, submon);
                    }
                    all(futs).then( (futArr, err0) -> {
                        IPromise futures[] = (IPromise[]) futArr;
                        for (int i = 0; i < futures.length; i++) {
                            IPromise future = futures[i];
                            subResult[i] = future.get();
                        }
                        p.settle(result, null);
                    });
                } else
                    p.settle(result, null);
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

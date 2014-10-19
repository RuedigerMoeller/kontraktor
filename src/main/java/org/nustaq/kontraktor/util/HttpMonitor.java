package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
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
     * method name gets part of url
     */
    public void report( String name, int depth, Callback cb ) {
        if ( depth == 0 ) {
            depth = 1;
        }
        Monitorable monitorable = monitored.get(name);
        if ( monitorable == null )
            cb.receive("no such monitorable registered:"+name, "not found");
        else {
            Object[] result = new Object[2];
            sendMonitorable(depth,result, monitorable);
            delayed( 10, () -> self().$sync().then(()->cb.receive(result, FINSILENT)) );
        }
    }

    protected void sendMonitorable(int depth, Object result[], Monitorable monitorable) {
//        System.out.println("dumpmon " + monitorable);
        monitorable.$getReport().then( ( report, err ) -> {
            result[0] = report;
            monitorable.$getSubMonitorables().then((monitorables, errmon) -> {
                if ( monitorables.length > 0 && depth >= 1 ) {
                    result[1] = new Object[monitorables.length];
                    for (int i = 0; i < monitorables.length; i++) {
                        Monitorable submon = monitorables[i];
                        Object subres[] = new Object[2];
                        ((Object[]) result[1])[i] = subres;
                        sendMonitorable(depth-1,subres, submon);
                    }
                }
            });
        });
    }

    HashMap<String,Monitorable> monitored = new HashMap();
    @Local
    public void $publish(String name, Monitorable toPublish) {
        monitored.put(name, toPublish );
    }

}

package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ruedi on 09.03.17.
 */
public class WapiRegistry extends Actor<WapiRegistry> {

    public static final String CONFIGUPDATE = "configupdate";
    public static final String SERVICEDUMP = "running";
    public static final String AVAILABLE = "available";
    public static final String TIMEOUT = "timeout";

    HashMap<String, List<WapiDescription>> services;
    List<Callback> listeners;
    WapiConfig config;

    @Local
    public void init() {
        services = new HashMap<>();
        listeners = new ArrayList<>();
        checkTimeout();
        config = WapiConfig.read();
        serviceDumper();
    }

    public void serviceDumper() {
        if ( ! isStopped() ) {
            try {
                Log.Info(this, "------");
                services.forEach((k, sd) -> Log.Info(this,""+sd) );
                Log.Info(this, "------");

                listeners.forEach( cb -> {
                    cb.stream(new Pair(SERVICEDUMP, services));
                });

                if ( WapiConfig.isDirty() ) {
                    config = WapiConfig.read();
                    listeners.forEach(cb -> cb.stream(new Pair(CONFIGUPDATE, config)));
                }
            } catch (Exception e) {
                Log.Error(this,e);
            }
            delayed(10000, () -> serviceDumper());
        }
    }

    public void registerService( WapiDescription desc, Actor remoteRef ) {
        List<WapiDescription> serviceList = getServiceList(desc.getName());
        serviceList.add(desc);
        desc.remoteRef(remoteRef);
        desc.receiveHeartbeat();
        if (serviceList.size()==1)
            broadcastAvailable(desc);
    }

    public IPromise<Map<String,WapiDescription>> getServiceMap() {
        HashMap<String,WapiDescription> servMap = new HashMap<>();
        services.forEach((name, list) -> {
            if (list.size() > 0)
                servMap.put(name, list.get(0));
        });
        return resolve(servMap);
    }

    public void subscribe( Callback<Pair<String,WapiDescription>> cb ) {
        listeners.add(cb);
    }

    protected void broadcastAvailable(WapiDescription desc) {
        Pair msg = new Pair(AVAILABLE,desc);
        listeners.forEach(cb -> {
            try {
                cb.stream(msg);
            } catch (Throwable th) {
                Log.Info(this, th);
            }
        });
    }

    protected void broadCastTimeOut(WapiDescription desc) {
        Pair msg = new Pair(TIMEOUT,desc);
        for (int i = 0; i < listeners.size(); i++) {
            Callback cb = listeners.get(i);
            try {
                cb.stream(msg);
            } catch (Throwable th) {
                Log.Info(this, th);
                listeners.remove(i);
                i--;
            }
        }
    }

    public IPromise<WapiConfig> getConfig() {
        return resolve(config);
    }

    public void receiveHeartbeat( String serviceName, String uniqueKey ) {
        getServiceList(serviceName).forEach(sdesc -> {
            if (sdesc.getUniqueKey().equals(uniqueKey)) {
                sdesc.receiveHeartbeat();
            }
        });
    }

    @Local public void checkTimeout() {
        services.values().forEach( list -> {
            int prevsiz = list.size();
            for (int i = 0; i < list.size(); i++) {
                WapiDescription serviceDescription = list.get(i);
                if ( serviceDescription.hasTimedOut() ) {
                    list.remove(i);
                    i--;
                    broadCastTimeOut(serviceDescription);
                }
            }
            // if services timed out, but there is a replacement,
            // broadcast availability
            if ( prevsiz != list.size() && list.size() > 0 ) {
                broadcastAvailable(list.get(0));
            }
        });
        if ( ! isStopped() ) {
            delayed(1000, () -> checkTimeout());
        }
    }

    protected List<WapiDescription> getServiceList(String serviceName) {
        List<WapiDescription> slist = services.get(serviceName);
        if ( slist == null ) {
            slist = new ArrayList<>();
            services.put(serviceName, slist);
        }
        return slist;
    }

}

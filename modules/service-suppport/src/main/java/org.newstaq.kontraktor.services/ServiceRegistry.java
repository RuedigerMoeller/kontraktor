package org.newstaq.kontraktor.services;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ruedi on 18/03/16.
 */
public class ServiceRegistry {

    public static final String SERVICEDUMP = "running";
    public static final String AVAILABLE = "available";
    public static final String TIMEOUT = "timeout";
    public static final int DEFAULT_PORT = 7654;
//    HashMap<String, List<ServiceDescription>> services;
//    List<Callback> listeners;
//    SystemCfg config;
//
//    @Local
//    public void init() {
//        services = new HashMap<>();
//        listeners = new ArrayList<>();
//        checkTimeout();
//        config = SystemCfg.read();
//        serviceDumper();
//    }
//
//    public void serviceDumper() {
//        if ( ! isStopped() ) {
//            System.out.println("---");
//            services.forEach((k, sd) -> System.out.println(sd));
//            System.out.println("---");
//
//            listeners.forEach( cb -> {
//                cb.stream(new Pair(SERVICEDUMP, services));
//            });
//
//            delayed(10000, () -> serviceDumper());
//        }
//    }
//
//    public void registerService( ServiceDescription desc ) {
//        List<ServiceDescription> serviceList = getServiceList(desc.getName());
//        serviceList.add(desc);
//        desc.receiveHeartbeat();
//        if (serviceList.size()==1)
//            broadcastAvailable(desc);
//    }
//
//    public IPromise<Map<String,ServiceDescription>> getServiceMap() {
//        HashMap<String,ServiceDescription> servMap = new HashMap<>();
//        services.forEach((name, list) -> {
//            if (list.size() > 0)
//                servMap.put(name, list.get(0));
//        });
//        return resolve(servMap);
//    }
//
//    public void subscribe( Callback<Pair<String,ServiceDescription>> cb ) {
//        listeners.add(cb);
//    }
//
//    protected void broadcastAvailable(ServiceDescription desc) {
//        Pair msg = new Pair(AVAILABLE,desc);
//        listeners.forEach(cb -> {
//            try {
//                cb.stream(msg);
//            } catch (Throwable th) {
//                Log.Info(this, th);
//            }
//        });
//    }
//
//    protected void broadCastTimeOut(ServiceDescription desc) {
//        Pair msg = new Pair(TIMEOUT,desc);
//        for (int i = 0; i < listeners.size(); i++) {
//            Callback cb = listeners.get(i);
//            try {
//                cb.stream(msg);
//            } catch (Throwable th) {
//                Log.Info(this, th);
//                listeners.remove(i);
//                i--;
//            }
//        }
//    }
//
//    public IPromise<SystemCfg> getConfig() {
//        return resolve(config);
//    }
//
//    public void receiveHeartbeat( String serviceName, String uniqueKey ) {
//        getServiceList(serviceName).forEach(sdesc -> {
//            if (sdesc.getUniqueKey().equals(uniqueKey)) {
//                sdesc.receiveHeartbeat();
//            }
//        });
//    }
//
//    @Local public void checkTimeout() {
//        services.values().forEach( list -> {
//            int prevsiz = list.size();
//            for (int i = 0; i < list.size(); i++) {
//                ServiceDescription serviceDescription = list.get(i);
//                if ( serviceDescription.hasTimedOut() ) {
//                    list.remove(i);
//                    i--;
//                    broadCastTimeOut(serviceDescription);
//                }
//            }
//            // if services timed out, but there is a replacement,
//            // broadcast availability
//            if ( prevsiz != list.size() && list.size() > 0 ) {
//                broadcastAvailable(list.get(0));
//            }
//        });
//        if ( ! isStopped() ) {
//            delayed(1000, () -> checkTimeout());
//        }
//    }
//
//    protected List<ServiceDescription> getServiceList(String serviceName) {
//        List<ServiceDescription> slist = services.get(serviceName);
//        if ( slist == null ) {
//            slist = new ArrayList<>();
//            services.put(serviceName, slist);
//        }
//        return slist;
//    }
//
//    public static ServiceArgs options;
//    public static ServiceArgs parseCommandLine(String[] args, ServiceArgs options) {
//
//        JCommander com = new JCommander();
//        com.addObject(options);
//        try {
//            com.parse(args);
//        } catch (Exception ex) {
//            System.out.println("command line error: '"+ex.getMessage()+"'");
//            options.help = true;
//        }
//        if ( options.help ) {
//            com.usage();
//            System.exit(-1);
//        }
//        return options;
//    }
//
//
//    public static void main(String[] args) {
//
//        options = parseCommandLine(args,new ServiceArgs());
//
//        ServiceRegistry sreg = Actors.AsActor(ServiceRegistry.class);
//        sreg.init();
//
//        new TCPNIOPublisher(sreg,DEFAULT_PORT).publish(actor -> {
//            Log.Info(null, actor + " has disconnected");
//        });
//
//        // log service activity
//        sreg.subscribe((pair, err) -> {
//            Log.Info(sreg.getClass(), pair.car() + " " + pair.cdr());
//        });
//
//    }
//
}

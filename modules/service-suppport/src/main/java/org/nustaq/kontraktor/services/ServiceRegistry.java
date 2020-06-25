package org.nustaq.kontraktor.services;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.WriterConfig;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.rest.FromQuery;
import org.nustaq.kontraktor.services.datacluster.DataShard;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataShard;
import org.nustaq.kontraktor.services.rlserver.SingleProcessRLClusterArgs;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 11.08.2015.
 *
 * a simple service registry. Services can be registered by name.
 * They are expected to send cyclical heartbeats.
 * By registering a listener (callback) remote services can listen to
 * availability/unavailability of services
 *
 * Rationale: in cluster of services managing connectivity gets complex
 * quickly. In order to simplify this, services are given a single point
 * of contact: the service registry (serviceRegistry). They register themself and
 * obtain address and availability from the central registry.
 * Note: Downside is, this is a SPOF.
 */
public class ServiceRegistry extends Actor<ServiceRegistry> {

    public static final String CONFIGUPDATE = "configupdate";
    public static final String SERVICEDUMP = "running";
    public static final String AVAILABLE = "available";
    public static final String TIMEOUT = "timeout";

    protected HashMap<String, List<ServiceDescription>> services;
    protected Map<String,StatusEntry> statusMap;
    protected List<Callback> listeners;
    protected ClusterCfg config;

    @Local
    public void init(ClusterCfg cfg) {
        services = new HashMap<>();
        statusMap = new HashMap<>();
        listeners = new ArrayList<>();
        checkTimeout();
        config = cfg == null ? ClusterCfg.read() : cfg;
        serviceDumper();
    }

    public void serviceDumper() {
        if ( ! isStopped() ) {
            try {
                Log.Info(this, "------");
                services.forEach((k, sd) -> Log.Info(this,""+sd) );
                Log.Info(this, "------");

                listeners.forEach( cb -> {
                    cb.pipe(new Pair(SERVICEDUMP, services));
                });

                if ( ClusterCfg.isDirty() ) {
                    config = ClusterCfg.read();
                    listeners.forEach(cb -> cb.pipe(new Pair(CONFIGUPDATE, config)));
                }
            } catch (Exception e) {
                Log.Error(this,e);
            }
            if ( options.dumpServices() )
                delayed(10000, () -> serviceDumper());
        }
    }

    public void registerService( ServiceDescription desc ) {
        Log.Info(this,"registering service "+desc);
        List<ServiceDescription> serviceList = getServiceList(desc.getName());
        serviceList.add(desc);
        desc.receiveHeartbeat();
        if (serviceList.size()==1)
            broadcastAvailable(desc);
    }

    public IPromise<Map<String,ServiceDescription>> getServiceMap() {
        HashMap<String,ServiceDescription> servMap = new HashMap<>();
        services.forEach((name, list) -> {
            if (list.size() > 0)
                servMap.put(name, list.get(0));
        });
        return resolve(servMap);
    }

    protected ServiceDescription getService(String name) {
        List<ServiceDescription> serviceList = getServiceList(name);
        if ( serviceList.size() > 0 )
            return serviceList.get(0);
        return null;
    }

    public void subscribe( Callback<Pair<String,ServiceDescription>> cb ) {
        listeners.add(cb);
    }

    protected void broadcastAvailable(ServiceDescription desc) {
        Pair msg = new Pair(AVAILABLE,desc);
        listeners = listeners.stream().filter( cb -> !cb.isTerminated()).collect(Collectors.toList());
        listeners.forEach(cb -> {
            try {
                cb.pipe(msg);
            } catch (Throwable th) {
                Log.Info(this, th);
            }
        });
    }

    protected void broadCastTimeOut(ServiceDescription desc) {
        Pair msg = new Pair(TIMEOUT,desc);
        for (int i = 0; i < listeners.size(); i++) {
            Callback cb = listeners.get(i);
            try {
                cb.pipe(msg);
            } catch (Throwable th) {
                Log.Info(this, th);
                listeners.remove(i);
                i--;
            }
        }
    }

    public IPromise<ClusterCfg> getConfig() {
        return resolve(config);
    }

    // backward compat
    public void receiveHeartbeat( String serviceName, String uniqueKey ) {
        receiveHeartbeatWithStatus(serviceName, uniqueKey,null);
    }

    public void receiveHeartbeatWithStatus( String serviceName, String uniqueKey, Serializable status ) {
        long now = System.currentTimeMillis();
        getServiceList(serviceName).forEach(sdesc -> {
            if (sdesc.getUniqueKey().equals(uniqueKey)) {
                sdesc.receiveHeartbeat();
                if ( status != null )
                    updateStatus(now, sdesc,uniqueKey+"#"+sdesc.getName(),status);
            }
        });
    }

    public static class StatusEntry implements Serializable {
        long time;
        long startUpTime;
        Object status;
        String key;
        String name;

        public StatusEntry(long time, Object status, String key, String name) {
            this.time = time;
            this.status = status;
            this.key = key;
            this.name = name;
        }

        public long getStartUpTime() {
            return startUpTime;
        }

        public long getTime() {
            return time;
        }

        public Object getStatus() {
            return status;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name == null ? "unnamed" : name;
        }
    }

    protected void removeStatus(String key) {
        statusMap.remove( key );
    }

    protected void updateStatus(long now, ServiceDescription td, String key, Serializable status) {
        StatusEntry prevStatusEntry = statusMap.get(key);
        StatusEntry newEntry = new StatusEntry(now, status, key, td.getName());
        if ( prevStatusEntry != null ) {
            newEntry.startUpTime = prevStatusEntry.startUpTime;
        } else
            newEntry.startUpTime = System.currentTimeMillis();
        statusMap.put( key, newEntry);
    }

    @Local public void checkTimeout() {
        services.values().forEach( list -> {
            int prevsiz = list.size();
            for (int i = 0; i < list.size(); i++) {
                ServiceDescription serviceDescription = list.get(i);
                if ( serviceDescription.hasTimedOut() ) {
                    list.remove(i);
                    removeStatus(serviceDescription.getUniqueKey()+"#"+serviceDescription.getName() );
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

    protected List<ServiceDescription> getServiceList(String serviceName) {
        List<ServiceDescription> slist = services.get(serviceName);
        if ( slist == null ) {
            slist = new ArrayList<>();
            services.put(serviceName, slist);
        }
        return slist;
    }

    public static RegistryArgs options;
    public static ServiceArgs parseCommandLine(String[] args, String concatArgs[], ServiceArgs options) {

        JCommander com = new JCommander();
        com.setAcceptUnknownOptions(true);
        com.addObject(options);
        try {
            com.parse(args);
            if ( concatArgs != null ) {
                parseCommandLine(concatArgs,null,options);
            }
        } catch (Exception ex) {
            System.out.println("command line error: '"+ex.getMessage()+"'");
            options.help = true;
        }
        if ( options.help ) {
            com.usage();
            System.exit(-1);
        }
        return options;
    }

    // for production hotfix, remove !
    public static ServiceArgs parseCommandLine(String[] args, ServiceArgs options) {

        JCommander com = new JCommander();
        com.setAcceptUnknownOptions(true);
        com.addObject(options);
        try {
            com.parse(args);
        } catch (Exception ex) {
            System.out.println("command line error: '"+ex.getMessage()+"'");
            options.help = true;
        }
        if ( options.help ) {
            com.usage();
            System.exit(-1);
        }
        return options;
    }

    private IPromise<RestApi> getRest() {
        RestApi restApi = AsActor(RestApi.class, getScheduler());
        restApi.init(self());
        return resolve(restApi);
    }

    public static class RestApi extends Actor<RestApi> {

        private ServiceRegistry reg;
        FSTConfiguration jsonConfiguration;

        public void init(ServiceRegistry reg) {
            this.reg = reg;
            jsonConfiguration = FSTConfiguration.createJsonConfiguration(true, false);
            jsonConfiguration.registerCrossPlatformClassMappingUseSimpleName(
                TableDescription.class,
                ServiceDescription.class,
                TCPConnectable.class,
                DataShard.class,
                SerializerType.class,
                Coding.class,
                WebSocketConnectable.class,
                Class.class,
                StatusEntry.class,
                DynDataShard.class
            );
        }

        public IPromise getBalance() {
            Promise p = new Promise();
            reg.balanceDynShards().then( (r,e) -> {
                if ( e == null )
                    p.resolve("<html>balancing done</html>");
                else
                    p.reject(e);
            });
            return p;
        }

        // does not work yet. drop node
        public IPromise getRelease(@FromQuery("shard") String shard ) {
            if ( reg.getServiceMap().await().get(shard) == null ) {
                return resolve("<html>unknown shard '"+shard+"' </html>");
            }
            reg.releaseDynShard(shard);
            return resolve("<html>released "+shard+" </html>");
        }

        public IPromise get() {
            return resolve("<html>try " +
                "<a href='/mon/services'>/mon/services</a>" +
                " or " +
                "<a href='/mon/stati'>/mon/stati</a>" +
                " or " +
                "<a href='/mon/distribution'>/mon/distribution</a>" +
                " or " +
                "<a href='/mon/activeDistribution'>/mon/activeDistribution</a>" +
                " or " +
                "<a href='/mon/balance'>/mon/balance</a>" +
                "</html>");
        }

        // GET ./services
        public IPromise getServices() {
            Promise p = new Promise();
            reg.getServiceMap().then( (r,e) -> {
                if ( r != null ) {
                    try {
                        p.resolve(new String(jsonConfiguration.asByteArray(r), "UTF-8") );
                    } catch (UnsupportedEncodingException e1) {
                        Log.Error(this,e1);
                        p.reject(500);
                    }
                } else {
                    p.reject(500);
                }
            });
            return p;
        }

        public IPromise getDistribution() {
            return resolve(reg.getDynDataDistribution().await().toJsonObj().toString(WriterConfig.PRETTY_PRINT));
        }

        public IPromise getActiveDistribution() {
            return resolve(reg.getActiveDynDataDistribution().await().toJsonObj().toString(WriterConfig.PRETTY_PRINT));
        }

        // GET ./stati
        public IPromise getStati() {
            Promise p = new Promise();
            reg.getStati().then( (r,e) -> {
                if ( r != null ) {
                    try {
                        p.resolve(new String(jsonConfiguration.asByteArray(r), "UTF-8") );
                    } catch (UnsupportedEncodingException e1) {
                        Log.Error(this,e1);
                        p.reject(500);
                    }
                } else {
                    p.reject(500);
                }
            });
            return p;
        }
    }

    /**
     * only valid on DynData cluster, get distribution as reported by datanodes
     *
     * @return
     */
    public IPromise<DynClusterDistribution> getDynDataDistribution() {
        return resolve(null);
    }

    /**
     * only valid on DynData cluster, get distribution as assumed by registry
     *
     * @return
     */
    public IPromise<DynClusterDistribution> getActiveDynDataDistribution() {
        return resolve(null);
    }
    /**
     * only valid on DynData cluster, rebalance data load
     *
     * @return
     */
    public IPromise balanceDynShards() {
        // empty see DynDataRegistry
        return resolve(null);
    }

    /**
     * drop node, remove all data !!not yet implemented!!
     * @param name
     * @return
     */
    public IPromise releaseDynShard(String name) {
        // empty see DynDataRegistry
        return resolve(null);
    }

    public IPromise<List<StatusEntry>> getStati() {
        List<StatusEntry> res = new ArrayList<>();
        statusMap.forEach( (k,v) ->  res.add(v) );
        res.sort( (a,b) -> a.getName().compareTo(b.getName()));
        return resolve(res);
    }

    public static void main(String[] args) {
        start(args);
    }

    public static ServiceRegistry start(String[] args) {
        options = (RegistryArgs) parseCommandLine(args,null,RegistryArgs.New());
        return start(options);
    }

    public static ServiceRegistry start(RegistryArgs options) {
        return start(options,null, ServiceRegistry.class);
    }

    public static void start(SingleProcessRLClusterArgs options, ClusterCfg cfg) {
        start(options,cfg,ServiceRegistry.class);
    }

    public static ServiceRegistry start(RegistryArgs _options, ClusterCfg cfg, Class<? extends ServiceRegistry> clazz) {
        options = _options;

        if ( ! _options.isAsyncLog() ) {
            Log.SetSynchronous();
        }

        if ( _options.getClustercfg() != null )
            ClusterCfg.pathname = _options.getClustercfg();

        ServiceRegistry serviceRegistry = Actors.AsActor(clazz);
        serviceRegistry.init(cfg);

        Log.Info(ServiceRegistry.class,"listening on "+_options.getRegistryHost()+" "+_options.getRegistryPort());
        new TCPNIOPublisher(serviceRegistry,_options.getRegistryPort()).publish(actor -> {
            Log.Info(null, actor + " has disconnected");
        });

        Log.Info(ServiceRegistry.class,"monport on http://"+_options.getMonhost()+":"+_options.getMonport()+"/mon");
        Http4K.Build(_options.getMonhost(), _options.getMonport() )
            .restAPI("/mon", serviceRegistry.getRest().await(), serviceRegistry.getReqAuth(), serviceRegistry.getPrepareResponse() )
            .build();

        // log service activity
        serviceRegistry.subscribe(( pair, err) -> {
            Log.Info(serviceRegistry.getClass(), pair.car() + " " + pair.cdr());
        });

        return serviceRegistry;
    }

    @CallerSideMethod
    public Consumer<HttpServerExchange> getPrepareResponse() {
        return null;
    }

    @CallerSideMethod
    public Function<HeaderMap, IPromise> getReqAuth() {
        return null;
    }


}

package org.nustaq.kontraktor.services;

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ReconnectableRemoteRef;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.kontraktor.services.datacluster.DataShard;
import org.nustaq.kontraktor.services.rlclient.dynamic.DynDataClient;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataShard;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.server.actors.DynTableSpaceActor;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.serialization.util.FSTUtil;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by ruedi on 12.08.2015.
 */
public abstract class ServiceActor<T extends ServiceActor> extends Actor<T> {

    public static final String REGISTRY_DISCONNECTED = "registry disconnected";
    public static final String REGISTRY_RECONNECTED = "registry reconnected";

    public static int DEFAULT_START_TIMEOUT = 60_000 * 10;

    public static ServiceActor RunTCP(String args[], Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceArgs> argsClazz) {
        return RunTCP(args,serviceClazz,argsClazz, DEFAULT_START_TIMEOUT);
    }

    public static ServiceActor RunTCP(String args[], Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceArgs> argsClazz, Class<? extends ServiceRegistry> serviceRegistryClass) {
        return RunTCP(args,serviceClazz, argsClazz, serviceRegistryClass, DEFAULT_START_TIMEOUT);
    }

    public static ServiceActor RunTCP( String args[], Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceArgs> argsClazz, long timeout) {
        return RunTCP(args,serviceClazz,argsClazz,ServiceRegistry.class,timeout);
    }

    /**
     * run & connect a service with given cmdline args and classes
     *
     * @param args
     * @param serviceClazz
     * @param argsClazz
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static ServiceActor RunTCP( String args[], Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceArgs> argsClazz, Class<? extends ServiceRegistry> serviceRegistryClass, long timeout) {
        ServiceArgs options = null;
        try {
            options = ServiceRegistry.parseCommandLine(args, null, argsClazz.newInstance());
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
        return RunTCP(options, serviceClazz, serviceRegistryClass, timeout);
    }
    public static ServiceActor RunTCP(ServiceArgs options, Class<? extends ServiceActor> serviceClazz, long timeout) {
        return RunTCP(options,serviceClazz,ServiceRegistry.class,timeout);
    }

    public static ServiceActor RunTCP(ServiceArgs options, Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceRegistry> serviceRegistryClass, long timeout) {
        ServiceActor myService = AsActor(serviceClazz);
        TCPConnectable connectable = new TCPConnectable(serviceRegistryClass, options.getRegistryHost(), options.getRegistryPort());

        myService.init( connectable, options, true).await(timeout);
        Log.Info(myService.getClass(), "Init finished");

        return myService;
    }

    public static final String UNCONNECTED = "UNCONNECTED";

    protected ReconnectableRemoteRef<ServiceRegistry> serviceRegistry;
    protected Map<String,Object> requiredServices; // also contains DynDataShards in case of dynamic clusters
    protected ClusterCfg config;
    protected ServiceDescription serviceDescription;
    protected ServiceArgs cmdline;
    protected DataClient dclient;
    protected DynClusterDistribution currentDistribution;
    IPromise initComplete;
    List<BiConsumer<String,Object>> serviceEventListener;

    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        initComplete = new Promise();
        this.cmdline = options;
        serviceEventListener = new ArrayList<>();

        if ( ! options.isAsyncLog() ) {
            Log.SetSynchronous();
        }

        Log.Info(this, "startup options " + options);
        Log.Info(this, "connecting to serviceRegistry ..");
        serviceRegistry = new ReconnectableRemoteRef<>(registryConnectable, new ReconnectableRemoteRef.ReconnectableListener() {
            @Override
            public void remoteDisconnected(Actor disconnected) {
                execute( () -> onRegistryDisconnected() );
            }

            @Override
            public void remoteConnected(Actor connected) {
                execute( () -> {
                    Log.Info(this,".. connected");
                    onRegistryConnected(autoRegister);
                });
            }

        });
        return initComplete;
    }

    /**
     * runs on client side, receives forwarded service event received from service actor
     *
     * @param l
     */
    public void addServiceEventListener(BiConsumer<String,Object> l) {
        if ( ! serviceEventListener.contains(l) )
            serviceEventListener.add(l);
    }

    public void removeServiceEventListener(BiConsumer<String,Object> l) {
        serviceEventListener.remove(l);
    }

    @CallerSideMethod public ServiceRegistry getServiceRegistry() {
        return (ServiceRegistry) getActor().serviceRegistry.get();
    }

    protected void fireServiceEvent(String ev, Object arg) {
        serviceEventListener.forEach( con -> con.accept(ev,arg));
    }

    protected void onRegistryDisconnected() {
        fireServiceEvent(REGISTRY_DISCONNECTED,null);
    }

    protected void onRegistryConnected(boolean autoRegister) {
        Log.Info(this, "connected serviceRegistry.");
        config = serviceRegistry.get().getConfig().await();

        boolean isReconnect = initComplete.isSettled();

        if ( isReconnect ) {
            onServiceRegistryReconnected();
            registerSelf();
        } else {
            Log.Info(this, "loaded cluster configuration");
            // FIXME: might want to close old services and resubscribe ..
            requiredServices = new HashMap<>();

            Arrays.stream(getAllServiceNames()).forEach(sname -> requiredServices.put(sname, UNCONNECTED));
            Log.Info(this, "waiting for required services ..");
            awaitRequiredServices().then((r, e) -> {
                if (e == null) {
                    if (isFixedDataCluster()) {
                        initRealLiveFixed(); // awaits
                    } else if ( isDynamicDataCluster() ) {
                        initRealLiveDynamic(); // awaits
                    }
                    Log.Info(this, "got all required services ..");
                    // all required services are there, now
                    // publish self as available service
                    if (autoRegister)
                        registerSelf();
                    initComplete.resolve();
                } else {
                    Log.Warn(this, "missing services " + e);
                }
            });
        }

    }

    protected void onServiceRegistryReconnected() {
        // re-register and resubscription is already handled
        // subclasse might want to update configuration and check if
        // services have been relocated
        fireServiceEvent(REGISTRY_RECONNECTED,null);
        Log.Info(this, "service registry reconnected.");
    }

    protected IPromise awaitRequiredServices() {
        Promise p = new Promise();
        Log.Info(this, "connecting required services ..");
        awaitRequiredServicesInternal(p);
        return p;
    }

    protected void awaitRequiredServicesInternal(Promise p) {
        connectRequiredServices().then( () -> {
            long missing = requiredServices.values().stream().filter(serv -> serv == UNCONNECTED).count();
            if ( missing > 0 ) {
                Log.Warn(this,"missing: ");
                requiredServices.forEach((name,serv) -> {
                    if ( serv == UNCONNECTED )
                        Log.Warn(this,"    "+name);
                });
                delayed(2000, () -> awaitRequiredServicesInternal(p));
            } else if ( isDynamicDataCluster() ) {
                // wait for valid cluster distribution
                ServiceRegistry serviceRegistry = this.serviceRegistry.get();
                if ( serviceRegistry instanceof DynDataServiceRegistry == false ) {
                    Log.Error(this,"Fatal: need DynDataServiceRegistry to manage dynamic data cluster");
                    delayed(1000, () -> System.exit(2));
                }
                DynDataServiceRegistry reg = (DynDataServiceRegistry) serviceRegistry;
                try {
                    DynClusterDistribution distribution = reg.getActiveDistribution().await();
                    if ( distribution != null ) {
                        Log.Info(this,"received distribution, start initializing dataclient ");
                        serviceRegistry.getServiceMap().then( (smap,err) -> {
                           if ( smap != null ) {
                               List proms = new ArrayList<>();
                               smap.values().stream()
                                   .filter( desc -> desc.getName().startsWith(DynDataShard.DATA_SHARD_NAME))
                                   .forEach( desc -> {
                                       Promise sp = new Promise();
                                       proms.add(sp);
                                       connectService(desc).then( (r,e) -> {
                                           if ( r != null ) {
                                               Log.Info(this,"dyndatacluster init connecting "+desc);
                                               requiredServices.put(desc.getName(),r);
                                               sp.resolve();
                                           } else {
                                               Log.Error(this, "failed to connect "+desc);
                                               sp.reject(e);
                                           }
                                       });
                                   });
                               allMapped(proms).await();
                               setCurrentDistribution(distribution);
                               p.resolve();
                           } else {
                               p.reject("could not aquire servicemap");
                           }
                        });
                    } else {
                        Log.Info(this,"wait for distribution map ..");
                        delayed(2000, () -> awaitRequiredServicesInternal(p));
                    }
                } catch (Exception e) {
                    Log.Error(this,e);
                }
            } else {
                p.resolve();
            }
        });
    }

    private void setCurrentDistribution(DynClusterDistribution distribution) {
        currentDistribution = distribution;
    }
    protected void initRealLiveDynamic() {
        dclient = InitRealLiveDynamic(currentDistribution,serviceRegistry.get(), name -> getService(name), self(), config.getDataCluster() );
    }

    protected void old_initRealLiveDynamic() {
        Log.Info(this, "init datacluster client");
        int nShards = currentDistribution == null ? 0 : currentDistribution.getNumberOfShards();
        Log.Info(this, "number of shards "+nShards);
        DynDataShard shards[] =  new DynDataShard[nShards];
        DynTableSpaceActor tsShard[] = new DynTableSpaceActor[nShards];
        Map<String, ServiceDescription> serviceMap = serviceRegistry.get().getServiceMap().await();
        int i = 0;
        for (Iterator<String> iterator = serviceMap.keySet().iterator(); iterator.hasNext(); ) {
            String serviceName =  iterator.next();
            if ( serviceName.startsWith(DynDataShard.DATA_SHARD_NAME) ) {
                shards[i] = getService(serviceName);
                if ( shards[i] == null ) {
                    Log.Error(this,"FATAL: announced shard not found/connected:"+serviceName);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(1);
                } else {
                    Log.Info(this, "connect to shard " + serviceName);
                    tsShard[i] = shards[i].getTableSpace().await();
                    tsShard[i].__clientsideTag = serviceName;
                }
                i++;
            }
        }
        if ( i != nShards )
        {
            Log.Error(this,"FATAL: number dyndatashards contradicts distribution");
            delayed(1000,() -> System.exit(1));
        }
        Log.Info(this, "dc connected all shards");

        dclient = Actors.AsActor(DynDataClient.class);
        ((DynDataClient) dclient).setInitialMapping(currentDistribution);
        dclient.connect(config.getDataCluster(),tsShard,self()).await(DEFAULT_START_TIMEOUT);
        Log.Info(this, "dc init done");
        Log.Info(this,"\n"+currentDistribution);
    }

    public static DynDataClient InitRealLiveDynamic(
        DynClusterDistribution currentDistribution,
        ServiceRegistry serviceRegistry,
        Function<String,DynDataShard> serviceMapper,
        ServiceActor hostingService /* can be null */,
        DataCfg schema
    ) {
        Log.Info(ServiceActor.class, "try init datacluster client");
        int nShards = currentDistribution == null ? 0 : currentDistribution.getNumberOfShards();
        if ( nShards == 0 )
            return null;
        Log.Info(ServiceActor.class, "number of shards "+nShards);
        DynDataShard shards[] =  new DynDataShard[nShards];
        DynTableSpaceActor tsShard[] = new DynTableSpaceActor[nShards];
        Map<String, ServiceDescription> serviceMap = serviceRegistry.getServiceMap().await();
        int i = 0;
        for (Iterator<String> iterator = serviceMap.keySet().iterator(); iterator.hasNext(); ) {
            String serviceName =  iterator.next();
            if ( serviceName.startsWith(DynDataShard.DATA_SHARD_NAME) ) {
                shards[i] = serviceMapper.apply(serviceName);
                if ( shards[i] == null ) {
                    Log.Error(ServiceActor.class,"FATAL: announced shard not found/connected:"+serviceName);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(1);
                } else {
                    Log.Info(ServiceActor.class, "connect to shard " + serviceName);
                    tsShard[i] = shards[i].getTableSpace().await();
                    tsShard[i].__clientsideTag = serviceName;
                }
                i++;
            }
        }
        if ( i != nShards )
        {
            Log.Error(ServiceActor.class,"FATAL: number dyndatashards contradicts distribution");
            SimpleScheduler.DelayedCall(1000,() -> System.exit(1));
        }
        Log.Info(ServiceActor.class, "dc connected all shards");

        DynDataClient dclient = Actors.AsActor(DynDataClient.class);
        ((DynDataClient) dclient).setInitialMapping(currentDistribution);
        dclient.connect(schema,tsShard,hostingService).await(DEFAULT_START_TIMEOUT);
        Log.Info(ServiceActor.class, "dc init done");
        Log.Debug(ServiceActor.class,"\n"+currentDistribution);
        return dclient;
    }

    protected void initRealLiveFixed() {
        Log.Info(this, "init datacluster client");
        int nShards = config.getDataCluster().getNumberOfShards();
        Log.Info(this, "number of shards "+nShards);
        DataShard shards[] =  new DataShard[nShards];
        TableSpaceActor tsShard[] = new TableSpaceActor[nShards];
        for ( int i = 0; i < nShards; i++ ) {
            shards[i] = getService(DataShard.DATA_SHARD_NAME + i);
            Log.Info(this,"connect to shard "+i);
            tsShard[i] = shards[i].getTableSpace().await();
            tsShard[i].__clientsideTag = DataShard.DATA_SHARD_NAME + i;
        }
        Log.Info(this, "dc connected all shards");

        dclient = Actors.AsActor(DataClient.class);
        dclient.connect(config.getDataCluster(),tsShard,self()).await(DEFAULT_START_TIMEOUT);

        Log.Info(this, "dc init done");
    }

    public IPromise<ClusterCfg> getConfig() {
        return resolve(config);
    }

    public IPromise<DataClient> getDataClient() {
        return resolve(dclient);
    }

    /**
     * register at service registry
     */
    protected void registerSelf() {
        publishSelf();
        serviceRegistry.get().registerService(getServiceDescription());
        serviceRegistry.get().subscribe((pair, err) -> {
            serviceEvent(pair.car(), pair.cdr(), err);
        });
        heartBeat();
        Log.Info(this, "registered at serviceRegistry.");
    }

    protected void publishSelf() {
        Log.Info(this,"publishing self "+getServiceDescription().getName());
        int defaultPort = getPort();
        // service does not expose itself
        if ( defaultPort <= 0 ) {
            Log.Warn(this,"Service "+getServiceDescription().getName()+" has no port and host configured. Unpublished.");
            return;
        }
        Log.Info(this,"publishing self at "+defaultPort);
        new TCPNIOPublisher(self(), defaultPort).publish(actor -> {
            Log.Info(null, actor + " has disconnected");
        });
    }

    /**
     * @return port this service wants to expose (with default tcp exposure)
     */
    protected int getPort() {
        return cmdline.getHostport();
    }

    /**
     * @return host this service wants to expose on (with default tcp exposure)
     */
    protected String getHost() {
        return cmdline.getHost();
    }

    protected ServiceArgs getCmdline() {
        return cmdline;
    }

    protected String[] getAllServiceNames() {
        if ( isFixedDataCluster() ) {
            String[] rn = getRequiredServiceNames();
            int numberOfShards = config.getDataCluster().getNumberOfShards();
            String s[] = Arrays.copyOf(rn,rn.length+numberOfShards);
            for (int i = 0; i < numberOfShards; i++) {
                s[i+rn.length] = DataShard.DATA_SHARD_NAME+i;
            }
            return s;
        }
        return getRequiredServiceNames();
    }

    protected boolean isFixedDataCluster() {
        return ! isDynamicDataCluster();
    }

    protected boolean isDynamicDataCluster() {
        return false;
    }

    protected abstract String[] getRequiredServiceNames();

    protected void serviceEvent(String eventId, Object cdr, Object err) {
        if ( cdr != null && ServiceRegistry.TIMEOUT.equals(eventId) && requiredServices.containsKey( ((ServiceDescription)cdr).getName()) ) {
            requiredSerivceWentDown((ServiceDescription) cdr);
        }
        if ( ServiceRegistry.CONFIGUPDATE.equals(eventId) ) {
            config = (ClusterCfg) cdr;
            notifyConfigChanged();
        }
        if ( DynDataServiceRegistry.RECORD_DISTRIBUTION.equals(eventId) ) {
            setCurrentDistribution((DynClusterDistribution) cdr);
        }
        fireServiceEvent(eventId,cdr);
    }

    /**
     * override, config contains updated ClusterCfg
     */
    protected void notifyConfigChanged() {

    }

    // ping based
    protected void requiredSerivceWentDown( ServiceDescription cdr ) {
        Log.Error(this,"required service went down. Shutting down. :"+cdr);
        self().stop();
    }

    protected <T extends Actor> T getService(String name) {
        Object service = requiredServices.get(name);
        if ( service == UNCONNECTED || service == null )
            return null;
        return (T) service;
    }

    /**
     * try to connect required (unconnected) services, in case of failur UNCONNECTED is put into service hashmap instead
     * @return
     */
    public IPromise connectRequiredServices() {
        if ( requiredServices.size() == 0 ) {
            return resolve();
        }
        IPromise res = new Promise<>();
        serviceRegistry.get().getServiceMap().then((smap, err) -> {
            List<IPromise<Object>> servicePromis = new ArrayList();
            String[] servNames = getAllServiceNames();
            for (int i = 0; i < servNames.length; i++) {
                String servName = servNames[i];
                ServiceDescription serviceDescription = smap.get(servName);
                if (serviceDescription != null && requiredServices.get(servName) instanceof Actor == false) {
                    if ( serviceDescription.getConnectable() == null ) {
                        Log.Error(this, "No connecteable defined for service "+serviceDescription.getName() );
                    }
                    IPromise connect;
                    try {
                        Log.Info(this,"connect "+serviceDescription.getConnectable());
                        connect = connectService(serviceDescription);
                    } catch (Throwable th) {
                        Log.Error(this, th, "failed to connect "+serviceDescription.getName() );
                        continue;
                    }
                    Promise notify = new Promise();
                    servicePromis.add(notify);

                    connect.then((actor, connectionError) -> {
                        if (actor != null) {
                            requiredServices.put(servName, actor);
                            Log.Info(this,"connected required service "+servName);
                            notify.complete();
                        } else {
                            requiredServices.put(servName,UNCONNECTED);
                            Log.Info(this,"connected requireed service "+servName);
                            Log.Warn(this, "failed to connect " + servName + " " + connectionError+" "+serviceDescription.getConnectable());
                            notify.reject("failed to connect " + servName + " " + connectionError);
                        }
                    });
                } else {
                    // wrong ! service already connected
                    //requiredServices.put(servName,UNCONNECTED);
                }
            }
            all(servicePromis).then( res );
        });
        return res;
    }

    protected IPromise<Actor> connectService(ServiceDescription serviceDescription) {
        return serviceDescription.getConnectable().connect(null, act -> serviceDisconnected(act) );
    }

    protected void serviceDisconnected(Actor act) {
        Log.Warn(this,"a remote service disconnected "+act );
        dclient.nodeDisconnected(act);
    }

    @Local
    public void heartBeat() {
        if ( isStopped() )
            return;
        if (serviceRegistry.isOnline()) {
            ServiceDescription sd = getServiceDescription();
            serviceRegistry.get().receiveHeartbeatWithStatus(sd.getName(), sd.getUniqueKey(), getStatus() );
            delayed(1000, () -> heartBeat());
        }
    }

    protected Serializable getStatus() {
        return null;
    }

    protected void gravityDisconnected() {
        serviceRegistry = null;
    }

    abstract protected ServiceDescription createServiceDescription();
    protected ServiceDescription getServiceDescription() {
        if ( serviceDescription == null )
            serviceDescription = createServiceDescription();
        return serviceDescription;
    }

    protected ConnectableActor createDefaultConnectable() {
        if ( getPort() >= 0 )
            return new TCPConnectable( getClass(), cmdline.getHost(), getPort() );
        return null;
    }

}

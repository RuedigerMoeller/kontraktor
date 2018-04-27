package org.nustaq.kontraktor.services;

import org.nustaq.kontraktor.remoting.base.ReconnectableRemoteRef;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.serialization.util.FSTUtil;

import java.util.*;

/**
 * Created by ruedi on 12.08.2015.
 */
public abstract class ServiceActor<T extends ServiceActor> extends Actor<T> {

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
    public static ServiceActor RunTCP( String args[], Class<? extends ServiceActor> serviceClazz, Class<? extends ServiceArgs> argsClazz) {
        ServiceActor myService = AsActor(serviceClazz);
        ServiceArgs options = null;
        try {
            options = ServiceRegistry.parseCommandLine(args, argsClazz.newInstance());
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
        TCPConnectable connectable = new TCPConnectable(ServiceRegistry.class, options.getRegistryHost(), options.getRegistryPort());

        myService.init( connectable, options, true).await(30_000);
        Log.Info(myService.getClass(), "Init finished");

        return myService;
    }

    public static final String UNCONNECTED = "UNCONNECTED";

    protected ReconnectableRemoteRef<ServiceRegistry> serviceRegistry;
    protected Map<String,Object> requiredServices;
    protected ClusterCfg config;
    protected ServiceDescription serviceDescription;
    protected ServiceArgs cmdline;
    protected DataClient dclient;

    IPromise initComplete;

    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        initComplete = new Promise();
        this.cmdline = options;

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
                    onRegistryConnected(autoRegister);
                });
            }

        });
        return initComplete;
    }

    protected void onRegistryDisconnected() {

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
                    if (needsDataCluster()) {
                        initRealLive(); // awaits
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
        Log.Info(this, "service registry reconnected.");
    }

    protected IPromise awaitRequiredServices() {
        Promise p = new Promise();
        awaitRequiredServicesInternal(p);
        return p;
    }

    private void awaitRequiredServicesInternal(Promise p) {
        connectRequiredServices().then( () -> {
            long missing = requiredServices.values().stream().filter(serv -> serv == UNCONNECTED).count();
            if ( missing > 0 ) {
                Log.Warn(this,"missing: ");
                requiredServices.forEach((name,serv) -> {
                    if ( serv == UNCONNECTED )
                        Log.Warn(this,"    "+name);
                });
                delayed(2000, () -> awaitRequiredServicesInternal(p));
            } else {
                p.resolve();
            }
        });
    }

    private void initRealLive() {
        Log.Info(this, "init datacluster client");
        int nShards = config.getDataCluster().getNumberOfShards();
        Log.Info(this, "number of shards "+nShards);
        DataShard shards[] =  new DataShard[nShards];
        TableSpaceActor tsShard[] = new TableSpaceActor[nShards];
        for ( int i = 0; i < nShards; i++ ) {
            shards[i] = getService(DataShard.DATA_SHARD_NAME + i);
            Log.Info(this,"connect to shard "+i);
            tsShard[i] = shards[i].getTableSpace().await();
        }
        Log.Info(this, "dc connected all shards");

        dclient = Actors.AsActor(DataClient.class);
        dclient.connect(config.getDataCluster(),tsShard,self()).await();

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

    protected int getPort() {
        return -1;
    }

    protected ServiceArgs getCmdline() {
        return cmdline;
    }

    protected String[] getAllServiceNames() {
        if ( needsDataCluster() ) {
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

    protected boolean needsDataCluster() {
        return true;
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
        Log.Info(this, "connecting required services ..");
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
                        connect = serviceDescription.getConnectable().connect(null, act -> serviceDisconnected(act) );
                    } catch (Throwable th) {
                        Log.Error(this, th, "failed to connect "+serviceDescription.getName() );
                        continue;
                    }
                    Promise notify = new Promise();
                    servicePromis.add(notify);

                    connect.then((actor, connectionError) -> {
                        if (actor != null) {
                            requiredServices.put(servName, actor);
                            notify.complete();
                        } else {
                            requiredServices.put(servName,UNCONNECTED);
                            Log.Warn(this, "failed to connect " + servName + " " + connectionError+" "+serviceDescription.getConnectable());
                            notify.reject("failed to connect " + servName + " " + connectionError);
                        }
                    });
                } else {
                    requiredServices.put(servName,UNCONNECTED);
                }
            }
            all(servicePromis).then( res );
        });
        return res;
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
            serviceRegistry.get().receiveHeartbeat(sd.getName(), sd.getUniqueKey());
            delayed(1000, () -> heartBeat());
        }
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

}

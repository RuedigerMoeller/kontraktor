package org.nustaq.kontraktor.services;

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

    protected ServiceRegistry serviceRegistry;
    protected Map<String,Object> requiredServices;
    protected ClusterCfg config;
    protected ServiceDescription serviceDescription;
    protected ServiceArgs cmdline;
    protected DataClient dclient;

    public IPromise init(ConnectableActor gravityConnectable, ServiceArgs options, boolean autoRegister) {
        this.cmdline = options;

        if ( ! options.isSysoutlog() ) {
            Log.SetSynchronous();
//            Log.Lg.setLogWrapper(new Log4j2LogWrapper(Log.Lg.getSeverity()));
        }

        Log.Info(this, "startup options " + options);
        Log.Info(this, "connecting to serviceRegistry ..");
        serviceRegistry = (ServiceRegistry) gravityConnectable.connect((conn, err) -> {
            Log.Warn(null,"serviceRegistry disconnected");
            gravityDisconnected();
        }).await();

        Log.Info(this, "connected serviceRegistry.");

        config = serviceRegistry.getConfig().await();

        Log.Info(this, "loaded cluster configuration");
        requiredServices = new HashMap<>();
        Arrays.stream(getAllServiceNames()).forEach(sname -> requiredServices.put(sname, UNCONNECTED));

        Log.Info(this, "waiting for required services ..");
        awaitRequiredServices().await();
        if (needsDataCluster()) {
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
        Log.Info(this, "got all required services ..");

        // all required services are there, now
        // publish self as available service
        if ( autoRegister )
            registerAtGravity();

        return resolve();
    }

    int eventKey = 1;

    public IPromise<ClusterCfg> getConfig() {
        return resolve(config);
    }

    public IPromise<DataClient> getDataClient() {
        return resolve(dclient);
    }

    protected void registerAtGravity() {
        publishSelf();
        serviceRegistry.registerService(getServiceDescription());
        serviceRegistry.subscribe((pair, err) -> {
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

    public IPromise awaitRequiredServices() {
        Log.Info(this, "connecting required services ..");
        if ( requiredServices.size() == 0 ) {
            return resolve();
        }
        IPromise res = new Promise<>();
        serviceRegistry.getServiceMap().then((smap, err) -> {
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
                        connect = serviceDescription.getConnectable().connect();
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
                    res.reject("required service "+servName+" not registered.");
                    return;
                }
            }
            if ( ! res.isSettled() ) {
                all(servicePromis).timeoutIn(15000)
                    .then(res)
                    .onTimeout(() -> {
                        // todo:retry
                        Log.Info(this, "failed to connect required services, retry");
                    });
            }
        });
        return res;
    }

    @Local
    public void heartBeat() {
        if ( isStopped() )
            return;
        if (serviceRegistry !=null) {
            ServiceDescription sd = getServiceDescription();
            serviceRegistry.receiveHeartbeat(sd.getName(), sd.getUniqueKey());
            delayed(3000, () -> heartBeat());
        } else {
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

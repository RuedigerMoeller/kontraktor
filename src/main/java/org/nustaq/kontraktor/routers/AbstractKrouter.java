package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.CallbackRefSerializer;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;

import java.util.*;
import java.util.function.BiFunction;

/**
 * base class for load balancers and failover proxies.
 *
 * Krouters are designed to run as standalone processes (similar e.g. nginx).
 * ServiceActors connect and register actively to be available. Clients connect to
 * the Krouter which then forwards/dispatches remote messages and results depending
 * on implemented strategy.
 * This way services are protected against any malicious attacks as they don't even
 * accept connections from the network (they are clients of the Krouter, the Krouter has no
 * prior knowledge of address of the krouter's address. E.g. its possible to have a public Krouter
 * running in the cloud and run the service behind a firewall (or even http proxy).
 *
 */
public abstract class AbstractKrouter<T extends AbstractKrouter> extends Actor<T> implements RemotedActor, ServingActor {

    public static final String SERVICE_UNAVAILABLE = "Service unavailable";
    /**
     * client connection loss detection can be slow (just needed for resource cleanup)
     */
    public static long CLIENT_PING_INTERVAL_MS = 5000L;

    protected HashMap<Object,Long> timeoutMap;
    protected HashMap<String,ConnectionRegistry> clients; // also services (timing from connect => registerService makes service detection unreliable)

    protected Set<Long> nextAliveRemoteActors;
    protected long lastSwitch;
    private boolean stateful = false;

    public IPromise router$RegisterService(Actor remoteRef, boolean stateful) {
        ((AbstractKrouter) getActor()).stateful = stateful;
        ((AbstractKrouter) getActorRef()).stateful = stateful;
        Log.Info(this, (this.stateful ? "stateful ":"")+"service registered ");
        return null; // must be overridden
    }

    @CallerSideMethod
    protected boolean isStateful() {
        return stateful;
    }

    @Local
    public abstract void router$handleServiceDisconnect(Actor disconnected );

    @Override
    public IPromise<Long> router$clientPing(long tim, long[] publishedActorIds) {
        for (int i = 0; i < publishedActorIds.length; i++) {
            long publishedActorId = publishedActorIds[i];
            nextAliveRemoteActors.add(publishedActorId);
        }
        if ( lastSwitch == 0 ) {
            lastSwitch = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            if ( now - lastSwitch > CLIENT_PING_INTERVAL_MS * 2) {
                Set<Long> tmp = this.nextAliveRemoteActors;
                getServices().forEach( serv -> tmp.add(serv.__remoteId));
                nextAliveRemoteActors = new HashSet<>();
                lastSwitch = now;
//                System.out.println("alive:");
//                tmp.forEach( l -> System.out.println("  "+l));
                ConnectionRegistry reg = connection.get();
                long alive[] = new long[tmp.size()];
                int idx = 0;
                for (Iterator<Long> iterator = tmp.iterator(); iterator.hasNext(); ) {
                    Long next = iterator.next();
                    alive[idx++] = next;
                }
                if ( reg != null ) {
                    RemoteCallEntry rce = new RemoteCallEntry(
                        0, 1,
                        "zzRoutingRefGC",
                        null,
                        reg.getConf().asByteArray(new Object[] {alive})
                    );
                    getServices().forEach( service -> {
                        forwardMultiCall(rce, service, reg, null, null);
                    });
                }
            }
        }
        return super.router$clientPing(tim, publishedActorIds);
    }

    @Local
    public void init() {
        timeoutMap = new HashMap<>();
        clients = new HashMap<>();
        nextAliveRemoteActors = new HashSet<>();
        delayed(getServicePingTimeout(), () -> cyclic( getServicePingTimeout(), () -> {pingServices(); return true;} ) );
        delayed(getClientPingTimeout(), () -> cyclic( getClientPingTimeout(), () -> {checkPingOnClients(); return true;} ) );
        delayed(CLIENT_PING_INTERVAL_MS*2, () -> cyclic( CLIENT_PING_INTERVAL_MS*2, () -> { timeoutMap.clear(); return true; } ) );
    }

    @Override @CallerSideMethod
    public boolean __dispatchRemoteCall(
        ObjectSocket objSocket, RemoteCallEntry rce,
        ConnectionRegistry clientRemoteRegistry, // registry of client connection
        List<IPromise> createdFutures, Object authContext,
        BiFunction<Actor, String, Boolean> callInterceptor, long delayCode)
    {
        boolean isCB = rce.getMethod() != null;
        if ( isCB && rce.getMethod().startsWith("router$") ) {
            return super.__dispatchRemoteCall(objSocket,rce,clientRemoteRegistry,createdFutures,authContext,callInterceptor, delayCode);
        }
//        if ( isCB ) {
//            getActor().responseCounter.count();
//        } else {
//            getActor().requestCounter.count();
//        }
//        getActor().trafficCounter.count(rce.getSerializedArgs().length);
        boolean success = dispatchRemoteCall(rce, clientRemoteRegistry);
        if ( ! success ) {
            if (rce.getCB() != null) {
                rce.getCB().reject(SERVICE_UNAVAILABLE);
            }
            if ( rce.getFutureKey() != 0 ) {
                RemoteCallEntry cbrce = createErrorPromiseResponse(rce, clientRemoteRegistry);
                clientRemoteRegistry.inFacadeThread( () -> {
                    clientRemoteRegistry.forwardRemoteMessage(cbrce);
                });
            }
        }
        return false;
    }

    @Local
    public void pingServices() {
        getServices().forEach( serv -> {
            serv.ping().then( r -> {
                timeoutMap.put(serv,System.currentTimeMillis());
            });
        });
        getServices().forEach( serv -> {
            Long tim = timeoutMap.get(serv);
            if (tim != null && System.currentTimeMillis() - tim > getServicePingTimeout() * 2) {
                Log.Info(this, "service timeout, closing " + serv);
                handleServiceDiscon(serv);
                if (serv.isPublished())
                    serv.close();
            }
        });
    }

    @Local
    public void checkPingOnClients() {
        long now = System.currentTimeMillis();
//        System.out.println("checkPingClients "+clients.size());
        clients.forEach( (id,reg) -> {
            if ( isService(reg) )
            {
                int debug = 1;
                // FIXME: services get stuck in clients collections as well as clients
                // which never did get through a ping. Currently only distinction between
                // a client and a serevice is wehter it pings or not (+wether it registers)
            } else {
                if ( now-reg.getLastRoutingClientPing() > getClientPingTimeout() ||
                     reg.isTerminated()
                ){
                    self().clientDisconnected(reg,id);
                }
            }
        });
    }

    private boolean isService(ConnectionRegistry reg) {
        return reg.getLastRoutingClientPing() == 0;
    }

    protected long getServicePingTimeout() {
        return 1000L;
    }
    protected long getClientPingTimeout() {
        return CLIENT_PING_INTERVAL_MS*2;
    }

    @CallerSideMethod
    protected void sendFailoverNotification(ConnectionRegistry clientRemoteRegistry) {
        getActor().sendFailoverNotificationInternal(clientRemoteRegistry);
    }

    protected void sendFailoverNotificationInternal(ConnectionRegistry registry) {
        RemoteCallEntry rce = new RemoteCallEntry(
            0, 0,
            "krouterTargetDidChange",
            null,
            registry.getConf().asByteArray(new Object[] {})
        );
        registry.inFacadeThread( () -> registry.forwardRemoteMessage(rce) );
    }

    protected abstract List<Actor> getServices();

    @CallerSideMethod
    protected RemoteCallEntry createErrorPromiseResponse(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry) {
        RemoteCallEntry cbrce = new RemoteCallEntry();
        cbrce.setReceiverKey(rce.getFutureKey());
        cbrce.setSerializedArgs(clientRemoteRegistry.getConf().asByteArray(new Object[]{null,SERVICE_UNAVAILABLE}));
        cbrce.setQueue(1);
        return cbrce;
    }

    @CallerSideMethod
    protected void forwardMultiCall(RemoteCallEntry rce, Actor remoteRef, ConnectionRegistry clientRemoteRegistry, boolean[] done, Callback[] selected ) {
        getActor().forwardMultiCallInternal(rce,remoteRef,clientRemoteRegistry, done, selected);
    }

    // tweak handling such it can deal with double results, runs in caller thread (but single threaded => remoteref registry)
    @CallerSideMethod
    protected void forwardMultiCallInternal(
        RemoteCallEntry rceIn, Actor remoteRef, ConnectionRegistry clientRemoteRegistry,
        boolean[] done, Callback[] selected )
    {
        RemoteCallEntry rce = rceIn.createCopy();
        Runnable toRun = () -> {
            ConnectionRegistry serviceRemoteReg = remoteRef.__self.__clientConnection;
            if ( rce.getReceiverKey() == 1 ) // targets krouter facade
                rce.setReceiverKey(remoteRef.__remoteId);
            if ( rce.getFutureKey() != 0 ) {
                long prevFuturekey = rce.getFutureKey();
                Promise p = new Promise();
                long cbid = serviceRemoteReg.registerPublishedCallback(p);
                rce.setFutureKey(-cbid);
                p.then( (r,e) -> {
                    self().execute(()->{
                        serviceRemoteReg.removePublishedObject(cbid);
                        // websocket close is unreliable
                        if ( serviceRemoteReg.isTerminated() ) {
                            handleServiceDiscon(remoteRef);
                            return;
                        }
                        if (!done[0]) {
                            done[0] = true;
                            RemoteCallEntry cbrce = (RemoteCallEntry) r;
                            cbrce.setReceiverKey(prevFuturekey);
                            clientRemoteRegistry.forwardRemoteMessage(cbrce);
                        }
                    });
                });
            }
            if ( rce.getCB() != null ) {
                Callback cb = rce.getCB();
                CallbackWrapper wrapperTrick[] = { null };
                CallbackWrapper wrapper = wrapperTrick[0] = new CallbackWrapper(self(), (r,e) -> {
                    if ( selected[0] == null ) {
                        selected[0] = wrapperTrick[0];
                    }
                    if ( selected[0] == wrapperTrick[0] )
                        cb.complete(r,e);
                });
                rce.setCB(wrapper);
            }
            // websocket close is unreliable
            if ( ! serviceRemoteReg.isTerminated() )
                serviceRemoteReg.forwardRemoteMessage(rce);
            else {
                handleServiceDiscon(remoteRef);
            }
        };
        if ( Thread.currentThread() != getCurrentDispatcher() )
            self().execute(toRun);
        else
            toRun.run();
    }

    /**
     * dispatch call to a service. (use forwardXX to send)
     * @param rce
     * @param clientRemoteRegistry
     * @return return false in case call could not be dispatched
     */
    @CallerSideMethod
    protected abstract boolean dispatchRemoteCall(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry );

    @CallerSideMethod
    protected void forwardCall(RemoteCallEntry rce, Actor remoteRef, ConnectionRegistry clientRemoteRegistry ) {
        getActor().forwardCallInternal(rce,remoteRef,clientRemoteRegistry);
    }

    // call from client to service
    // mission: patch remotecall ids such everything is mappe via Krouter registry
    protected void forwardCallInternal(RemoteCallEntry rce, Actor remoteRef, ConnectionRegistry clientRemoteRegistry ) {
        RemoteCallEntry finalRce = rce.createCopy();
        Runnable toRun = () -> {
            ConnectionRegistry serviceRemoteReg = (ConnectionRegistry) remoteRef.__self.__clientConnection;
            if ( finalRce.getReceiverKey() == 1 ) // targets krouter facade
                finalRce.setReceiverKey(remoteRef.__remoteId);
            if (finalRce.getFutureKey() != 0) {
                long prevFuturekey = finalRce.getFutureKey();
                Promise p = new Promise();
                long cbid = serviceRemoteReg.registerPublishedCallback(p);
                finalRce.setFutureKey(-cbid);
                p.then((r, e) -> {
                    serviceRemoteReg.removePublishedObject(cbid);
                    // websocket close is unreliable
                    if ( serviceRemoteReg.isTerminated() ) {
                        handleServiceDiscon(remoteRef);
                        return;
                    }
                    RemoteCallEntry cbrce = (RemoteCallEntry) r;
                    cbrce.setReceiverKey(prevFuturekey);
                    self().execute(
                        () -> clientRemoteRegistry.forwardRemoteMessage(cbrce)
                    );
                });
            }
            if (finalRce.getCB() != null) {
                CallbackRefSerializer.MyRemotedCallback cb = (CallbackRefSerializer.MyRemotedCallback) finalRce.getCB();
                long id = cb.getId();
                finalRce.setCB( new CallbackWrapper(self(), (res,err) -> {
                    RemoteCallEntry rcerouted = (RemoteCallEntry) res;
                    rcerouted.setReceiverKey(id);
                    clientRemoteRegistry.forwardRemoteMessage(rcerouted);
                }){
                    @Override
                    public boolean isRouted() {
                        return true;
                    }
                });
            }
            // websocket close is unreliable
            if ( ! serviceRemoteReg.isTerminated() )
                serviceRemoteReg.forwardRemoteMessage(finalRce);
            else {
                handleServiceDiscon(remoteRef);
            }
        };
        if ( Thread.currentThread() != getCurrentDispatcher() )
            self().execute(toRun);
        else
            toRun.run();
    }

    @CallerSideMethod
    protected void handleServiceDiscon(Actor remoteRef) {
        // todo remove from clients hasmap
        self().router$handleServiceDisconnect(remoteRef);
    }

    public void hasBeenUnpublished(String connectionIdentifier) {
        Log.Info(this,"Krouter lost client "+connectionIdentifier);
    }

    public void clientConnected(ConnectionRegistry connectionRegistry, String connectionIdentifier) {
        Log.Info(this,"client connected "+connectionIdentifier);
        clients.put(connectionIdentifier,connectionRegistry);
    }

    public void clientDisconnected(ConnectionRegistry connectionRegistry, String connectionIdentifier) {
        Log.Info(this,"client disconnected "+connectionIdentifier);
        clients.remove(connectionIdentifier,connectionRegistry);
    }

}

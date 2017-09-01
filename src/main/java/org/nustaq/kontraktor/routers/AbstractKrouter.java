package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

import java.util.List;
import java.util.function.BiFunction;

/**
 * base class for load balancers and failover proxies.
 *
 * Krouters are designed to run as standalone processes (similar e.g. nginx).
 * ServiceActors connect and register actively to be available. Clients connect to
 * the Krouter which then forwards/dispatches remote messages and results depending
 * on implemented strategy.
 * This way services are protected against any malicious attacks as the don't even
 * accept connections from the network (they are clients of the Krouter, the Krouter has no
 * prior knowledge of address of the krouter's address. E.g. its possible to have a public Krouter
 * running in the cloud and run the service behind a firewall (or even http proxy).
 *
 * Note there is the assumption all services connecting have the same async interface.
 *
 * (use separate Krouters published on distinct url pathes (WebSockets or Http) in order to have one process support
 * more than one type of Service)
 *
 */
public abstract class AbstractKrouter<T extends AbstractKrouter> extends Actor<T> implements RemotedActor {

    public static final String SERVICE_UNAVAILABLE = "Service unavailable";

    @Local
    public abstract void init();

    @Local
    public abstract void router$handleDisconnect( Actor disconnected );

    @Override @CallerSideMethod
    public boolean __dispatchRemoteCall(
        ObjectSocket objSocket, RemoteCallEntry rce,
        RemoteRegistry clientRemoteRegistry, // registry of client connection
        List<IPromise> createdFutures, Object authContext,
        BiFunction<Actor, String, Boolean> callInterceptor)
    {
        if ( rce.getMethod() != null && rce.getMethod().startsWith("router$") ) {
            return super.__dispatchRemoteCall(objSocket,rce,clientRemoteRegistry,createdFutures,authContext,callInterceptor);
        }
        boolean success = dispatchRemoteCall(rce, clientRemoteRegistry);
        if ( ! success ) {
            if (rce.getCB() != null) {
                rce.getCB().reject(SERVICE_UNAVAILABLE);
            }
            if ( rce.getFutureKey() != 0 ) {
                RemoteCallEntry cbrce = createErrorPromiseResponse(rce, clientRemoteRegistry);
                clientRemoteRegistry.forwardRemoteMessage(cbrce);
            }
        }
        return false;
    }

    @CallerSideMethod
    protected RemoteCallEntry createErrorPromiseResponse(RemoteCallEntry rce, RemoteRegistry clientRemoteRegistry) {
        RemoteCallEntry cbrce = new RemoteCallEntry();
        cbrce.setReceiverKey(rce.getFutureKey());
        cbrce.setSerializedArgs(clientRemoteRegistry.getConf().asByteArray(new Object[]{null,SERVICE_UNAVAILABLE}));
        cbrce.setQueue(1);
        return cbrce;
    }

    @CallerSideMethod
    protected void forwardMultiCall(RemoteCallEntry rce, Actor remoteRef, RemoteRegistry clientRemoteRegistry,boolean[] done, Callback[] selected ) {
        getActor().forwardMultiCallInternal(rce,remoteRef,clientRemoteRegistry, done, selected);
    }

    // tweak handling such it can deal with double results, runs in caller thread (but single threaded => remoteref registry)
    @CallerSideMethod
    protected void forwardMultiCallInternal(
        RemoteCallEntry rceIn, Actor remoteRef, RemoteRegistry clientRemoteRegistry,
        boolean[] done, Callback[] selected )
    {
        RemoteCallEntry rce = rceIn.createCopy();
        Runnable toRun = () -> {
            RemoteRegistry serviceRemoteReg = (RemoteRegistry) remoteRef.__self.__clientConnection;
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
                            router$handleDisconnect(remoteRef);
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
                router$handleDisconnect(remoteRef);
            }
        };
        if ( Thread.currentThread() != getCurrentDispatcher() )
            self().execute(toRun);
        else
            toRun.run();
    }

    /**
     * dispatch call to a service.
     * @param rce
     * @param clientRemoteRegistry
     * @return return false in case call could not be dispatched
     */
    @CallerSideMethod
    protected abstract boolean dispatchRemoteCall(RemoteCallEntry rce, RemoteRegistry clientRemoteRegistry );

    @CallerSideMethod
    protected void forwardCall(RemoteCallEntry rce, Actor remoteRef, RemoteRegistry clientRemoteRegistry ) {
        getActor().forwardCallInternal(rce,remoteRef,clientRemoteRegistry);
    }

    // call from client to service
    // mission: patch remotecall ids such everything is mappe via Krouter registry
    protected void forwardCallInternal(RemoteCallEntry rce, Actor remoteRef, RemoteRegistry clientRemoteRegistry ) {
        RemoteCallEntry finalRce = rce.createCopy();
        Runnable toRun = () -> {
            RemoteRegistry serviceRemoteReg = (RemoteRegistry) remoteRef.__self.__clientConnection;
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
                        router$handleDisconnect(remoteRef);
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
                Callback cb = finalRce.getCB();
                CallbackWrapper wrapper = new CallbackWrapper(self(), cb);
                finalRce.setCB(wrapper);
            }
            // websocket close is unreliable
            if ( ! serviceRemoteReg.isTerminated() )
                serviceRemoteReg.forwardRemoteMessage(finalRce);
            else {
                router$handleDisconnect(remoteRef);
            }
        };
        if ( Thread.currentThread() != getCurrentDispatcher() )
            self().execute(toRun);
        else
            toRun.run();
    }

    public void hasBeenUnpublished(String connectionIdentifier) {
        System.out.println("Krouter lost client "+connectionIdentifier);
    }

    public static <T extends AbstractKrouter> T start(Class<T> krouterClass, ActorPublisher ... publisher) {
        T res = AsActor(krouterClass);
        res.init();
        for (int i = 0; i < publisher.length; i++) {
            ActorPublisher actorPublisher = publisher[i].facade(res);
            actorPublisher.publish( act -> {
                res.router$handleDisconnect(act);
            });
        }
        return res;
    }

}

package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 13.03.17.
 *
 * Simply forwards all incoming calls to the registered Service. If a new service registers, it replaces
 * the previous one.
 *
 * If a service is 'stateful', clients stick to their initial connected service instance as long
 * it is available. Only once the initially connected service instance becomes unavailable,
 * clients receive a failovernotification to re-establish state (e.g. subscriptions).
 *
 */
public abstract class SingleActiveServiceKrouter<T extends SingleActiveServiceKrouter> extends AbstractKrouter<T> {

    @CallerSideMethod
    protected abstract Actor getRemoteRef();

    @CallerSideMethod
    protected abstract void setRemoteRef(Actor remoteRef);

    @Override
    public IPromise router$RegisterService(Actor remoteRef, boolean stateful) {
        super.router$RegisterService(remoteRef,stateful);
        setRemoteRef(remoteRef);
        return resolve();
    }

    @Override
    public void init() {
        super.init();
    }

    @Local
    abstract public void router$handleServiceDisconnect(Actor x);

    @Override
    protected List<Actor> getServices() {
        List<Actor> svs = new ArrayList<>();
        if ( getRemoteRef() != null )
            svs.add(getRemoteRef());
        return svs;
    }

    @Override @CallerSideMethod
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry) {
        KrouterRemoteConUserData ud = null;
        if ( isStateful() ) { // stateful actors should to stick with initialservice if possible
            if ( clientRemoteRegistry.userData.get() == null ) {
                clientRemoteRegistry.userData.set(new KrouterRemoteConUserData());
            }
            ud = (KrouterRemoteConUserData) clientRemoteRegistry.userData.get();
            if ( ud.lastRoutedService != null ) {
                boolean published = ud.lastRoutedService.isPublished();
                boolean stopped = ud.lastRoutedService.isStopped();
                if (!stopped) {
                    forwardCall(rce, ud.lastRoutedService, clientRemoteRegistry);
                    return true;
                } else {
                    clientRemoteRegistry.userData.set(null);
                    sendFailoverNotification(clientRemoteRegistry);
                }
            }
        }
        if ( getRemoteRef() == null ) {
            Log.Warn(this,"unhandled call, service has disconnected");
            return false;
        } else {
            if ( ud != null )
                ud.lastRoutedService = getRemoteRef();
            willDispatch();
            forwardCall(rce, getRemoteRef(), clientRemoteRegistry);
        }
        return true;
    }

    @CallerSideMethod
    protected void willDispatch() {

    }

    public static void main(String[] args) {
        Routing.start(
            SingleActiveServiceKrouter.class,
            new TCPNIOPublisher()
                .port(6667)
                .serType(SerializerType.JsonNoRef)
        );
    }

}

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
 * Simply forwards all incoming calls to the registered Service
 *
 */
public class SimpleKrouter<T extends SimpleKrouter> extends AbstractKrouter<T> {

    protected Actor remoteRef;

    @Override
    public IPromise router$RegisterService(Actor remoteRef) {
        this.remoteRef = remoteRef;
        self().remoteRef = remoteRef;
        return resolve();
    }

    @Override
    public void init() {
        super.init();
    }

    @Local
    public void router$handleServiceDisconnect(Actor x) {
        remoteRef = null;
    }

    @Override
    protected List<Actor> getServices() {
        List<Actor> svs = new ArrayList<>();
        if ( remoteRef != null )
            svs.add(remoteRef);
        return svs;
    }

    @Override @CallerSideMethod
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry) {
        if ( remoteRef == null ) {
            Log.Warn(this,"unhandled call, service has disconnected");
            return false;
        } else
            forwardCall(rce,remoteRef,clientRemoteRegistry);
        return true;
    }

    public static void main(String[] args) {
        Routing.start(
            SimpleKrouter.class,
            new TCPNIOPublisher()
                .port(6667)
                .serType(SerializerType.JsonNoRef)
        );
    }

}

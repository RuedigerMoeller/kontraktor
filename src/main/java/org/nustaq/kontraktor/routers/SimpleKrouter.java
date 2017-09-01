package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by ruedi on 13.03.17.
 *
 * Simply forwards all incoming calls to the registered Service
 *
 */
public class SimpleKrouter<T extends SimpleKrouter> extends AbstractKrouter<T> {

    protected Actor remoteRef;

    public IPromise router$Register(Actor remoteRef) {
        this.remoteRef = remoteRef;
        self().remoteRef = remoteRef;
        return resolve();
    }

    @Override
    public void init() {

    }

    @Local
    public void router$handleDisconnect(Actor x) {
        remoteRef = null;
    }

    @Override @CallerSideMethod
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, RemoteRegistry clientRemoteRegistry) {
        if ( remoteRef == null ) {
            Log.Warn(this,"unhandled call, service has disconnected");
            return false;
        } else
            forwardCall(rce,remoteRef,clientRemoteRegistry);
        return true;
    }

    public static void main(String[] args) {
        start(
            SimpleKrouter.class,
            new TCPNIOPublisher()
                .port(6667)
                .serType(SerializerType.JsonNoRef)
        );
    }

}

package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 13.03.17.
 */
public class WapiForwarder extends Actor<WapiForwarder>  {

    Actor remoteRef;

    public void remoteRef(Actor remoteRef) {
        this.remoteRef = remoteRef;
    }

    AtomicLong promId = new AtomicLong(2);

    @Override @CallerSideMethod
    public boolean __dispatchRemoteCall(ObjectSocket objSocket, RemoteCallEntry rce, RemoteRegistry registry, List createdFutures) {
        RemoteRegistry remoteReg = (RemoteRegistry) getActor().remoteRef.__clientConnection;
        if ( rce.getFutureKey() > 0 ) {
            long prevFuturekey = rce.getFutureKey();
            Promise p = new Promise();
            long cbid = remoteReg.registerPublishedCallback(p);
            rce.setFutureKey(-cbid);
            p.then( (r,e) -> {
                int debug = 1;
                RemoteCallEntry cbrce = (RemoteCallEntry) r;
                cbrce.setReceiverKey(prevFuturekey);
                registry.forwardRemoteMessage(cbrce);
            });
        }
        if ( rce.getCB() != null ) {
            Callback cb = rce.getCB();
            rce.setCB( new CallbackWrapper( self(), cb ) );
        }
        remoteReg.forwardRemoteMessage(rce);
        return false;
    }

}

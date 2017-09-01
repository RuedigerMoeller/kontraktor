package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

/**
 * Simple HotCold failover router - several service instances might connect, but only
 * first one is used (as long it is connected). Once this instance disconnect, the next one is used.
 *
 * Note that implementing routing (+tweaks) require to break idiomatic concurrency handling of kontraktor, anyway this
 * is not visible to outer world.
 *
 */
public class HotColdFailoverKrouter extends HotHotFailoverKrouter<HotColdFailoverKrouter> {

    @Override @CallerSideMethod
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, RemoteRegistry clientRemoteRegistry) {
        if ( getActor().remoteServices.size() == 0 )
            return false;
        // attention: breaking threading contract here ! (see immutable add in register)
        forwardCall(rce, (Actor) getActor().remoteServices.get(0), clientRemoteRegistry);
        return true;
    }

}

package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

import java.util.ArrayList;

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
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry) {
        ArrayList<Actor> remoteServices = getActor().remoteServices;
        if ( remoteServices.size() == 0 )
            return false;
        // attention: breaking threading contract here ! (see immutable add in register)
        forwardCall(rce, (Actor) remoteServices.get(0), clientRemoteRegistry);
        return true;
    }

}

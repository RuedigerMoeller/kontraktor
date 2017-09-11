package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * A load balancing Krouter. Forwards round robin. Enables dynamic scaling of a
 * service.
 *
 * Clients of stateful services stick to initially connected instance (unless this fails)
 */
public class RoundRobinKrouter extends HotColdFailoverKrouter<RoundRobinKrouter> {

    int count = 0;

    @CallerSideMethod
    protected Actor getRemoteRef() {
        List<Actor> remoteServices = getActor().remoteServices;
        if ( remoteServices.size() == 0 )
            return null;
//        System.out.println("count:"+getActor().count);
        int count = getActor().count;
        if ( count >= remoteServices.size() ) {
            count = 0;
        }
        return remoteServices.get(count);
    }

    @Override @CallerSideMethod
    protected void willDispatch() {
        getActor().count++;
        if ( getActor().count >= getActor().remoteServices.size() )
            getActor().count = 0;
    }

}

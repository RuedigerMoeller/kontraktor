package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

import java.util.ArrayList;

/**
 * A load balancing Krouter. Forwards round robin. Enables dynamic scaling of a
 * service.
 */
public class RoundRobinKrouter extends HotHotFailoverKrouter<RoundRobinKrouter> {

    int count = 0;
    @Override @CallerSideMethod
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, RemoteRegistry clientRemoteRegistry) {
        ArrayList<Actor> remoteServices = getActor().remoteServices;
        if ( remoteServices.size() == 0 )
            return false;
        // attention: breaking threading contract here ! (see immutable add in register)
        count++;
        if ( count >= remoteServices.size() ) {
            count = 0;
        }
        forwardCall(rce, remoteServices.get(count), clientRemoteRegistry);
        return true;
    }

}

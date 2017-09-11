package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;

/**
 * a Krouter supporting dynamic replacement of a single service. For stateful service's,
 * clients keep connected to the service instance they originally
 * connected. Once this old service terminates they failover to the "current" service instance.
 *
 * THIS IS NOT A FAILOVER: if the primary (least recently connected) service goes down, no new service
 * can connect ('service unavailable), however clients connected to a previous service instance will continue working.
 * Use HotColdFailover for dynamic failover + dynamic replacement use cases.
 *
 * Use case: Replacement / Zero Downtime software update. In general its favourable to always
 * use HotColdFailover instead of SimpleKrouter, as this also covers dynamic replacement. (SimpleKrouter's
 * orinigal role was having a simple case for testing debugging).
 *
 */
public class SimpleKrouter extends SingleActiveServiceKrouter<SimpleKrouter> {

    Actor remRef;

    @CallerSideMethod
    protected Actor getRemoteRef() {
        return getActor().remRef;
    }

    @CallerSideMethod
    protected void setRemoteRef(Actor remoteRef) {
        getActor().remRef = remoteRef;
        self().remRef = remoteRef;
    }

    @Local
    public void router$handleServiceDisconnect(Actor x) {
        if ( x.getActor() == getRemoteRef() || x.getActorRef() == getRemoteRef() )
            setRemoteRef(null);
    }


}

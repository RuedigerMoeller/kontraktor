package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple HotCold failover router - several service instances might connect, but only
 * first one is used (as long it is connected). Once this instance disconnect, the next one is used.
 *
 * Note that implementing routing (+tweaks) require to break idiomatic concurrency handling of kontraktor, anyway this
 * is not visible to outer world.
 *
 */
public class HotColdFailoverKrouter<T extends HotColdFailoverKrouter> extends SingleActiveServiceKrouter<T> {

    List<Actor> remoteServices;

    public void init() {
        remoteServices = new ArrayList<>();
        super.init();
    }

    @Local
    public void router$handleServiceDisconnect(Actor x) {
        //FIXME: reply pending callbacks / promises with error
        List<Actor> newList = remoteServices.stream()
            .filter(rs -> rs != x.getActor() && rs != x.getActorRef())
            .collect(Collectors.toList());
        boolean remove = newList.size() != remoteServices.size();
        if ( ! remove ) {
            // can happen multiple times
        } else {
            remoteServices = newList;
            Log.Info(this, "removed service "+x);
        }
    }

    @CallerSideMethod
    protected Actor getRemoteRef() {
        List<Actor> remoteServices = getActor().remoteServices;
        if ( remoteServices.size() == 0 )
            return null;
        return remoteServices.get(0);
    }

    @CallerSideMethod
    protected void setRemoteRef(Actor remoteRef) {
        ArrayList services = new ArrayList();
        services.add(remoteRef);
        services.addAll(getActor().remoteServices);
        getActor().remoteServices = services;
        Log.Info(this,"service added. #services "+services.size());
    }

    @Override
    protected List<Actor> getServices() {
        return getActor().remoteServices;
    }


}

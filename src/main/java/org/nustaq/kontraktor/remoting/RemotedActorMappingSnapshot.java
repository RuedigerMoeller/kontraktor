package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.RemoteConnection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 24/04/15.
 *
 * used to snapshot current remote mapping to allow for reconnect on a different connection
 */
public class RemotedActorMappingSnapshot {

    Map<Integer,Actor> actorMapping = new HashMap<>();

    public Map<Integer, Actor> getActorMapping() {
        return actorMapping;
    }

    public void setActorMapping(Map<Integer, Actor> actorMapping) {
        this.actorMapping = actorMapping;
    }

    public void merge(RemoteConnection remoteCon, RemotedActorMappingSnapshot remotedActorMappingSnapshot) {
        actorMapping.putAll(remotedActorMappingSnapshot.getActorMapping());
    }
}

package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;

import java.util.HashMap;
import java.util.Map;

/**
 * not threadsafe !
 */
public class KConnectionPool {

    Map<String, Actor> connections = new HashMap<>();
    Map<String, IPromise> underway = new HashMap<>();

    public IPromise getConnection(ConnectableActor con) {
        String key = con.getKey();
        Actor actor = connections.get(key);
        if ( actor != null && ! actor.isStopped() ) {
            return new Promise(actor);
        }
        Promise res = new Promise();
        if ( underway.containsKey(key) ) {
            Promise nextpromise = (Promise) underway.get(key);
            IPromise then = nextpromise.getLast().then(res);
//            underway.put(key, then);
            return res;
        }
        underway.put(key, new Promise<>());
        con.connect( (cn,e) -> {}, act -> {
            connections.remove(key);
        }).then( (remote,err) -> {
            if ( remote != null )
                connections.put(key,remote);
            IPromise nextpromise = underway.get(key);
            if ( nextpromise != null )
                nextpromise.complete(remote,err);
            underway.remove(key);
            res.complete(remote,err);
        });
        return res;
    }

    public void closeAll() {
        Map<String,Actor> oldMap = connections;
        connections = new HashMap<>();
        oldMap.values().forEach( actor -> actor.unpublish() );
    }

}

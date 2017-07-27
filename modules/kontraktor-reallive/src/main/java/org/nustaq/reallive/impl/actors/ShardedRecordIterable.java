package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.RealLiveStreamActor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 05.08.2015.
 */
public class ShardedRecordIterable<K> implements RealLiveStreamActor {

    List<RealLiveStreamActor> shards = new ArrayList();

    @Override
    public <T> void forEach(Spore<Record, T> spore) {
        for (int i = 0; i < shards.size(); i++) {
            RealLiveStreamActor kvRecordIterable = shards.get(i);
            kvRecordIterable.forEach(spore);
        }
    }

    public ShardedRecordIterable<K> addShard(RealLiveStreamActor ri) {
        shards.add(ri);
        return this;
    }

    public void removeShard(RealLiveStreamActor ri) {
        shards.add(ri);
    }

}

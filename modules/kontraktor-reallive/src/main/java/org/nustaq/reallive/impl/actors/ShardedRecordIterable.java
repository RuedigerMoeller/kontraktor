package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.RealLiveStreamActor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 05.08.2015.
 */
public class ShardedRecordIterable<K> implements RealLiveStreamActor<K> {

    List<RealLiveStreamActor<K>> shards = new ArrayList<>();

    @Override
    public <T> void forEach(Spore<Record<K>, T> spore) {
        for (int i = 0; i < shards.size(); i++) {
            RealLiveStreamActor<K> kvRecordIterable = shards.get(i);
            kvRecordIterable.forEach(spore);
        }
    }

    public ShardedRecordIterable<K> addShard(RealLiveStreamActor<K> ri) {
        shards.add(ri);
        return this;
    }

    public void removeShard(RealLiveStreamActor<K> ri) {
        shards.add(ri);
    }

}

package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.RecordIterable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 05.08.2015.
 */
public class ShardedRecordIterable<K> implements RecordIterable<K> {

    List<RecordIterable<K>> shards = new ArrayList<>();

    @Override
    public <T> void forEach(Spore<Record<K>, T> spore) {
        for (int i = 0; i < shards.size(); i++) {
            RecordIterable<K> kvRecordIterable = shards.get(i);
            kvRecordIterable.forEach(spore);
        }
    }

    public ShardedRecordIterable<K> addShard(RecordIterable<K> ri) {
        shards.add(ri);
        return this;
    }

    public void removeShard(RecordIterable<K> ri) {
        shards.add(ri);
    }

}

package org.nustaq.reallive.actors;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.RecordIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by ruedi on 05.08.2015.
 */
public class ShardedRecordIterable<K,V extends Record<K>> implements RecordIterable<K,V> {

    List<RecordIterable<K,V>> shards = new ArrayList<>();

    @Override
    public void forEach(Predicate<V> filter, Consumer<V> action) {
        for (int i = 0; i < shards.size(); i++) {
            RecordIterable<K, V> kvRecordIterable = shards.get(i);
            kvRecordIterable.forEach( filter, action );
        }
    }

    public ShardedRecordIterable<K,V> addShard(RecordIterable<K,V> ri) {
        shards.add(ri);
        return this;
    }

    public void removeShard(RecordIterable<K,V> ri) {
        shards.add(ri);
    }

}

package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by ruedi on 03/08/15.
 */
public class HeapRecordStorage<K> implements RecordStorage<K> {

    Map<K,Record<K>> map;

    public HeapRecordStorage() {
        map = new HashMap<>();
    }

    public HeapRecordStorage(Map<K,Record<K>> map) {
        this.map = map;
    }

    @Override
    public RecordStorage put(K key, Record<K> value) {
        map.put(key,value);
        return this;
    }

    @Override
    public Record<K> get(K key) {
        return map.get(key);
    }

    @Override
    public Record<K> remove(K key) {
        return map.remove(key);
    }

    @Override
    public long size() {
        return map.size();
    }


    @Override
    public <T> void forEach(Spore<Record<K>, T> spore) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<K, Record<K>>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<K, Record<K>> next = iterator.next();
            spore.remote(next.getValue());
            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }

    public Map<K,Record<K>> getMap() {
        return map;
    }

    @Override
    public StorageStats getStats() {
        StorageStats stats = new StorageStats()
            .capacity(-1)
            .freeMem(-1)
            .usedMem(-1)
            .numElems(map.size());
        return stats;
    }

    @Override
    public Stream<Record<K>> stream() {
        return map.entrySet().stream().map( en -> en.getValue() );
    }


}

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
public class HeapRecordStorage implements RecordStorage {

    Map<Object,Record> map;

    public HeapRecordStorage() {
        map = new HashMap();
    }

    public HeapRecordStorage(Map<Object,Record> map) {
        this.map = map;
    }

    @Override
    public RecordStorage put(String key, Record value) {
        map.put(key,value);
        return this;
    }

    @Override
    public Record get(String key) {
        return map.get(key);
    }

    @Override
    public Record remove(String key) {
        return map.remove(key);
    }

    @Override
    public long size() {
        return map.size();
    }


    @Override
    public <T> void forEach(Spore<Record, T> spore) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<Object, Record>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Object, Record> next = iterator.next();
            spore.remote(next.getValue());
            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }

    public Map<Object,Record> getMap() {
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
    public Stream<Record> stream() {
        return map.entrySet().stream().map( en -> en.getValue() );
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        // do nothing
    }


}

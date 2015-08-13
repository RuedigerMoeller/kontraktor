package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ruedi on 03/08/15.
 */
public class HeapRecordStorage<K> implements RecordStorage<K> {

    Map<K,Record<K>> map = new HashMap<>();

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
        for (Iterator iterator = map.values().iterator(); iterator.hasNext(); ) {
            Record<K> record = (Record<K>) iterator.next();
            spore.remote(record);
            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }

    public Map<K,Record<K>> getMap() {
        return map;
    }
}

package org.nustaq.reallive.storage;

import org.nustaq.reallive.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by ruedi on 03/08/15.
 */
public class HeapRecordStorage<K,V extends Record<K>> implements RecordStorage<K,V> {

    Map<K,V> map = new HashMap<>();

    @Override
    public RecordStorage put(K key, V value) {
        map.put(key,value);
        return this;
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V remove(K key) {
        return map.remove(key);
    }

    @Override
    public void forEach(Consumer<V> action) {
        map.values().forEach(action);
    }
}

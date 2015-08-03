package org.nustaq.reallive.newimpl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by ruedi on 03/08/15.
 */
public class HeapStore<K,V extends Record<K>> implements RecordStore<K,V> {

    Map<K,V> map = new HashMap<>();

    @Override
    public RecordStore put(K key, V value) {
        map.put(key,value);
        return this;
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void forEach(Consumer<V> action) {
        map.values().forEach(action);
    }
}

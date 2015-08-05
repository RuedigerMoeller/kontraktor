package org.nustaq.reallive.storage;

import org.nustaq.reallive.api.*;

import java.util.function.*;

/**
 * Created by moelrue on 05.08.2015.
 */
public class OffHeapRecordStorage<V extends Record<String>> implements RecordStorage<String,V> {

    @Override
    public RecordStorage put(String key, V value) {
        return null;
    }

    @Override
    public V get(String key) {
        return null;
    }

    @Override
    public V remove(String key) {
        return null;
    }

    @Override
    public void forEach(Consumer<V> action) {

    }
}

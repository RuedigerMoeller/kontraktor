package org.nustaq.reallive.interfaces;


import org.nustaq.reallive.impl.storage.StorageStats;

import java.util.stream.Stream;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStorage<K> extends RecordIterable<K> {

    RecordStorage put( K key, Record<K> value );
    Record<K> get( K key );
    Record<K> remove( K key );
    long size();
    StorageStats getStats();
    Stream<Record<K>> stream();

}

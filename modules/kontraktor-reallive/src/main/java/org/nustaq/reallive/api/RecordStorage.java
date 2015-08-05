package org.nustaq.reallive.api;


/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStorage<K, V extends Record<K>> extends RecordStreamProvider<K,V> {

    RecordStorage put( K key, V value );
    V get( K key );
    V remove( K key );

}

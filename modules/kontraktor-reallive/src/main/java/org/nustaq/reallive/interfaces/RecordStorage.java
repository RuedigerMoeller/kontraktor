package org.nustaq.reallive.interfaces;


/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStorage<K, V extends Record<K>> extends RecordIterable<K,V> {

    RecordStorage put( K key, V value );
    V get( K key );
    V remove( K key );
    long size();

}

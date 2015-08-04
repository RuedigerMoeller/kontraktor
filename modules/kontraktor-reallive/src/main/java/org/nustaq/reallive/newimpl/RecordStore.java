package org.nustaq.reallive.newimpl;


/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStore<K, V extends Record<K>> extends RecordStreamProvider<K,V> {

    RecordStore put( K key, V value );
    V get( K key );
    V remove( K key );

}

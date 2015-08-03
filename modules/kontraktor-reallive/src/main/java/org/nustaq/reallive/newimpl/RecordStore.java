package org.nustaq.reallive.newimpl;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStore<K, V extends Record<K>> {

    RecordStore put( K key, V value );
    V get( K key );
    V remove( K key );

    void forEach(Consumer<V> action);

}

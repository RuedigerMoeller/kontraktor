package org.nustaq.reallive.newimpl;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStore<K,T extends Record<K>> {

    T get( K key );
    void forEach(Consumer<T> action);

}

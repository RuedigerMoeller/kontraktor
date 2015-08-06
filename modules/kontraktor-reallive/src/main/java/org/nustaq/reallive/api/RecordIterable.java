package org.nustaq.reallive.api;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RecordIterable<K,V extends Record<K>> {

    void forEach(Predicate<V> filter, Consumer<V> action);

}

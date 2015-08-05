package org.nustaq.reallive.api;

import java.util.function.Consumer;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RecordStreamProvider<K,V extends Record<K>> {

    void forEach(Consumer<V> action);

}

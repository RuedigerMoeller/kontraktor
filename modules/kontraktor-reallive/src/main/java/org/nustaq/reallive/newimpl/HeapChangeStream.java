package org.nustaq.reallive.newimpl;

import java.util.*;
import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class HeapChangeStream<K,V extends Record<K>> implements RecordStore<K,V>, ChangeReceiver<K,V> {

    HashMap<K,V> map = new HashMap<>();

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void forEach(Consumer<V> action) {

    }

    @Override
    public void receive(ChangeMessage<K, V> change) {

    }
}

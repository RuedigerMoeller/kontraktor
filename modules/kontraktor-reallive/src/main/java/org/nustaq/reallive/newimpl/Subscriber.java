package org.nustaq.reallive.newimpl;

import java.util.function.Predicate;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber<K,V extends Record<K>> {

    Predicate<V> filter;
    ChangeReceiver<K,V> receiver;

    public Subscriber(Predicate<V> filter, ChangeReceiver<K, V> receiver) {
        this.filter = filter;
        this.receiver = receiver;
    }

    public Predicate<V> getFilter() {
        return filter;
    }

    public ChangeReceiver<K, V> getReceiver() {
        return receiver;
    }
}

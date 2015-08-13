package org.nustaq.reallive.interfaces;

import java.util.function.Predicate;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber<K> {

    Predicate<Record<K>> filter;
    ChangeReceiver<K> receiver;

    public Subscriber(Predicate<Record<K>> filter, ChangeReceiver<K> receiver) {
        this.filter = filter;
        this.receiver = receiver;
    }

    public Predicate<Record<K>> getFilter() {
        return filter;
    }

    public ChangeReceiver<K> getReceiver() {
        return receiver;
    }
}

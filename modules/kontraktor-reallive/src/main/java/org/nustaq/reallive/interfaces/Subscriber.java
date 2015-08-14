package org.nustaq.reallive.interfaces;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber<K> implements Serializable {

    static AtomicInteger idCount = new AtomicInteger(0);

    RLPredicate<Record<K>> filter;
    ChangeReceiver<K> receiver;
    int id;

    public Subscriber(RLPredicate<Record<K>> filter, ChangeReceiver<K> receiver) {
        this.filter = filter;
        this.receiver = receiver;
        id = idCount.incrementAndGet();
    }

    public int getId() {
        return id;
    }

    public RLPredicate<Record<K>> getFilter() {
        return filter;
    }

    public ChangeReceiver<K> getReceiver() {
        return receiver;
    }
}

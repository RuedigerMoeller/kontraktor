package org.nustaq.reallive.api;

import org.nustaq.kontraktor.Callback;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber implements Serializable {

    static AtomicInteger idCount = new AtomicInteger(0);

    final RLPredicate<Record> filter;

    ChangeReceiver receiver;
    int id;
    transient Callback serverSideCB;

    public Subscriber(RLPredicate<Record> filter, ChangeReceiver receiver) {
        this.filter = filter == null ? rec -> true : filter;
        this.receiver = receiver;
        id = idCount.incrementAndGet();
    }

    public int getId() {
        return id;
    }

    public RLPredicate<Record> getFilter() {
        return filter;
    }

    public ChangeReceiver getReceiver() {
        return receiver;
    }

    public Subscriber serverSideCB(final Callback serverSideCB) {
        this.serverSideCB = serverSideCB;
        return this;
    }

    public Callback getServerSideCB() {
        return serverSideCB;
    }
}

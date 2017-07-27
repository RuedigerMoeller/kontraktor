package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber implements Serializable {

    static AtomicInteger idCount = new AtomicInteger(0);

    final RLPredicate<Record> filter;
    final RLPredicate<Record> prePatchFilter;

    ChangeReceiver receiver;
    int id;
    transient Callback serverSideCB;

    public Subscriber(RLPredicate<Record> prepatch, RLPredicate<Record> filter, ChangeReceiver receiver) {
        this.filter = filter == null ? rec -> true : filter;
        this.receiver = receiver;
        this.prePatchFilter = prepatch == null ? rec -> true : prepatch;
        id = idCount.incrementAndGet();
    }

    public RLPredicate<Record> getPrePatchFilter() {
        return prePatchFilter;
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

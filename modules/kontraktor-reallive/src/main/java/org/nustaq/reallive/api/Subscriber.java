package org.nustaq.reallive.api;

import org.nustaq.kontraktor.Callback;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 04/08/15.
 */
public class Subscriber implements Serializable {

    static AtomicInteger idCount = new AtomicInteger(0);

    RLPredicate<Record> filter;

    ChangeReceiver receiver;
    int id;
    transient Callback serverSideCB;
    transient Object userObject;

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


    // internal
    public Subscriber serverSideCB(final Callback serverSideCB) {
        this.serverSideCB = serverSideCB;
        return this;
    }

    // internal
    public Callback getServerSideCB() {
        return serverSideCB;
    }

    public Subscriber idCount(AtomicInteger idCount) {
        this.idCount = idCount;
        return this;
    }

    public Subscriber filter(RLPredicate<Record> filter) {
        this.filter = filter;
        return this;
    }

    public Subscriber receiver(ChangeReceiver receiver) {
        this.receiver = receiver;
        return this;
    }

    public Subscriber id(int id) {
        this.id = id;
        return this;
    }

    public Subscriber userObject(Object userObject) {
        this.userObject = userObject;
        return this;
    }

    public Object getUserObject() {
        return userObject;
    }
}

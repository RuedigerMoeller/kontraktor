package org.nustaq.reallive.api;

import java.io.Serializable;

/**
 * limits the amount of tested records per datanode. This can be useful to avoid transmission of all matching
 * records in find-alike scenarios.
 */
public class LimitedQuery implements Serializable, RLPredicate<Record> {

    private int counter;

    private final RLPredicate<Record> predicate;

    public LimitedQuery(final int limit, final RLPredicate<Record> predicate) {
        this.counter = limit;
        this.predicate = predicate;
    }

    @Override
    public boolean test(final Record record) {
        if (counter <= 0) {
            return false;
        }

        final boolean test = predicate.test(record);
        if (test) {
            counter--;
        }
        return test;
    }
}
package org.nustaq.reallive.api;

import java.io.Serializable;

/**
 * limits the amount of tested records per datanode. This can be useful to avoid transmission of all matching
 * records in find-alike scenarios.
 *
 * Query will be executed on each data node.
 * Therefore, max. result size will be `limit * amount` of data nodes
 * Example:
 * limit 5, node count: 2, max result size can be 10 items
 * - dataNode1 returns 5 items (limit is fulfilled)
 * - dataNode2 returns 3 items (more does not match sub predicate)
 * result: 8 items
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
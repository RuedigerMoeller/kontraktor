package org.nustaq.reallive.api;

public class RLLimitedPredicate<T> implements RLPredicate<T> {

    int limit;
    RLPredicate query;

    public RLLimitedPredicate(int limit, RLPredicate query) {
        this.limit = limit;
        this.query = query;
    }

    @Override
    public boolean test(T o) {
        return query.test(o);
    }

    @Override
    public int getRecordLimit() {
        return limit;
    }

    public void _setLimit(int limit) {
        this.limit = limit;
    }
}

package org.nustaq.reallive.api;

/**
 * a rate limited query. it limits the amount of results per second to the given value
 * @param <T>
 */
public class RLRateLimitedPredicate<T> implements RLPredicate<T> {

    int limit;
    RLPredicate query;

    public RLRateLimitedPredicate(int limit, RLPredicate query) {
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

package org.nustaq.reallive.impl;

import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.Subscriber;
import org.nustaq.reallive.query.CompiledQuery;
import org.nustaq.reallive.query.EvalContext;
import org.nustaq.reallive.query.Query;

/**
 * Created by ruedi on 29/08/15.
 */
public class QueryPredicate<T> implements RLPredicate<T> {

    String query;
    transient CompiledQuery compiled;

    public QueryPredicate(String query) {
        this.query = query;
    }

    @Override
    public boolean test(T t) {
        if ( compiled == null ) {
            compiled = Query.compile(query);
        }
        final boolean res = compiled.evaluate((EvalContext) t).isTrue();
        return res;
    }
}

package org.nustaq.reallive.impl;

import org.nustaq.reallive.api.RLPredicate;
import org.nustaq.reallive.query.CompiledQuery;
import org.nustaq.reallive.query.EvalContext;
import org.nustaq.reallive.query.Query;

/**
 * Created by ruedi on 29/08/15.
 */
public class QueryPredicate<T> implements RLPredicate<T> {

    String query;
    CompiledQuery compiled;

    public QueryPredicate(String query) {
        this.query = query;
        compiled = Query.compile(query);
    }

    public CompiledQuery getCompiled() {
        return compiled;
    }

    @Override
    public boolean test(T t) {
        final boolean res = compiled.evaluate((EvalContext) t).isTrue();
        return res;
    }
}

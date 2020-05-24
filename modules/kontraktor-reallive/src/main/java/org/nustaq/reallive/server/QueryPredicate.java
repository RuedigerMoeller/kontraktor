package org.nustaq.reallive.server;

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
        if ( compiled.getHashIndex() != null )
            compiled.getHashIndex().subQuery(this);
    }

    public CompiledQuery getCompiled() {
        return compiled;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public boolean test(T t) {
        final boolean res = compiled.evaluate((EvalContext) t).isTrue();
        return res;
    }
}

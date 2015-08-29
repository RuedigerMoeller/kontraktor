package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.reallive.impl.FilterSpore;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.query.CompiledQuery;
import org.nustaq.reallive.query.Query;

import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RecordIterable<K> {

    <T> void forEach(Spore<Record<K>,T> spore);

    @CallerSideMethod default void filter( RLPredicate<Record<K>> predicate, Callback cb ) {
        forEach(new FilterSpore<>(predicate).forEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default void query(String query, Callback cb) throws ParseException {
        this.filter(new QueryPredicate<Record<K>>(query), cb);
    }

}

package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.impl.FilterSpore;
import org.nustaq.reallive.impl.QueryPredicate;

import java.text.ParseException;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RecordIterable<K> {

    <T> void forEach(Spore<Record<K>,T> spore);

    /**
     *
     * @param prePatch - a filter applied to original record (no patching allowed)
     * @param predicate - a filter which gets a private copy (patching allowed)
     * @param cb
     */
    @CallerSideMethod default void filterPP( RLPredicate<Record<K>> prePatch, RLPredicate<Record<K>> predicate, Callback<Record<K>> cb ) {
        forEach(new FilterSpore<>(predicate,null).setForEach(cb).onFinish( () -> cb.finish() ));
    }
    @CallerSideMethod default void filter( RLPredicate<Record<K>> predicate, Callback<Record<K>> cb ) {
        forEach(new FilterSpore<>(predicate,null).setForEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default void query(String query, Callback<Record<K>> cb) throws ParseException {
        this.filter(new QueryPredicate<Record<K>>(query), cb);
    }

}

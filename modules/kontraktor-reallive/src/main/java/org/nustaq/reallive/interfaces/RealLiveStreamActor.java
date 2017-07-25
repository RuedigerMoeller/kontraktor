package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.impl.FilterSpore;
import org.nustaq.reallive.impl.MapSpore;
import org.nustaq.reallive.impl.QueryPredicate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RealLiveStreamActor {

    <T> void forEach(Spore<Record,T> spore);

    /**
     *
     * @param prePatch - a filter applied to original record (no patching allowed)
     * @param predicate - a filter which gets a private copy (patching allowed)
     * @param cb
     */
    @CallerSideMethod default void filterPP(RLPredicate<Record> prePatch, RLPredicate<Record> predicate, Callback<Record> cb ) {
        forEach(new FilterSpore(predicate,null).setForEach(cb).onFinish( () -> cb.finish() ));
    }
    @CallerSideMethod default void filter(RLPredicate<Record> predicate, Callback<Record> cb ) {
        forEach(new FilterSpore(predicate,null).setForEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default <R> void map(RLPredicate<Record> predicate, RLFunction<Record,R> mapFun, Callback<R> cb ) {
        forEach(new MapSpore(predicate,mapFun).setForEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default void query(String query, Callback<Record> cb) throws ParseException {
        this.filter(new QueryPredicate<Record>(query), cb);
    }

    @CallerSideMethod default <REC> IPromise<List<REC>> collect(RLPredicate<Record> predicate, Function<Record,REC> map) {
        Promise res = new Promise();
        ArrayList resl = new ArrayList();
        filter( predicate, (r,e) -> {
            if ( r != null ) {
                resl.add(map.apply(r));
            }
            if ( Actors.isComplete(e) ) {
                res.resolve(resl);
            }
        });
        return res;
    }

}

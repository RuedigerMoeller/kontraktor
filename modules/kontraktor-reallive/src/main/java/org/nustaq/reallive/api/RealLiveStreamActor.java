package org.nustaq.reallive.api;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.server.FilterSpore;
import org.nustaq.reallive.server.MapSpore;
import org.nustaq.reallive.server.QueryPredicate;
import org.nustaq.reallive.query.QParseException;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ruedi on 04/08/15.
 *
 * query methods
 */
public interface RealLiveStreamActor extends SafeRealLiveStreamActor {

    <T> void forEachWithSpore(Spore<Record,T> spore);

    @CallerSideMethod default void forEach(RLPredicate<Record> predicate, Callback<Record> cb ) {
        forEachWithSpore(new FilterSpore(predicate).setForEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default <R> void map(RLPredicate<Record> predicate, RLFunction<Record,R> mapFun, Callback<R> cb ) {
        forEachWithSpore(new MapSpore(predicate,mapFun).setForEach(cb).onFinish( () -> cb.finish() ));
    }

    @CallerSideMethod default void query(String query, Callback<Record> cb) throws QParseException {
        this.forEach(new QueryPredicate<Record>(query), cb);
    }

    @CallerSideMethod default IPromise<List<Record>> queryList(String query) {
        Promise prom = new Promise();
        List<Record> res = new ArrayList<>();
        query(query, (r,e) -> {
            if ( r != null ) {
                res.add(r);
            } else {
                prom.complete(res,e);
            }
        });
        return prom;
    }

    /**
     * sends null,null as marker for finish (so null cannot be a valid result of the spore)
     */
    @CallerSideMethod default <O> void forEach(Spore<Record,O> spore, Callback<O> result) {
        spore.setForEach(result).onFinish( () -> result.complete(null,null) );
        forEachWithSpore(spore);
    }

}

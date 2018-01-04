package org.nustaq.reallive.api;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.impl.FilterSpore;
import org.nustaq.reallive.impl.MapSpore;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.query.QParseException;


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

}

package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.query.CompiledQuery;
import org.nustaq.reallive.query.Query;

import java.text.ParseException;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K> {

    void subscribe( Subscriber<K> subs );

    default @CallerSideMethod
    Subscriber<K> subscribeOn(RLPredicate<Record<K>> filter, ChangeReceiver<K> receiver) {
        Subscriber<K> subs = new Subscriber<>(null,filter,receiver);
        this.subscribe(subs);
        return subs;
    }

    /**
     *
     * @param prePatchfilter - cannot modify
     * @param filter - can modify record (private copy)
     * @param receiver
     * @return
     */
    default @CallerSideMethod
    Subscriber<K> subscribeOn(RLPredicate<Record<K>> prePatchfilter, RLPredicate<Record<K>> filter, ChangeReceiver<K> receiver) {
        Subscriber<K> subs = new Subscriber<>(prePatchfilter,filter,receiver);
        this.subscribe(subs);
        return subs;
    }

    default @CallerSideMethod
    Subscriber<K> subscribeOn(String query, ChangeReceiver<K> receiver) throws ParseException {
        Subscriber<K> subs = new Subscriber<>(null,new QueryPredicate<>(query),receiver);
        this.subscribe(subs);
        return subs;
    }

    void unsubscribe( Subscriber<K> subs );

}

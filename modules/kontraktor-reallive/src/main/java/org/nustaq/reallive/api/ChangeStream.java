package org.nustaq.reallive.api;

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.query.QParseException;


/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream extends SafeChangeStream {

    void subscribe( Subscriber subs );

    default @CallerSideMethod
    Subscriber subscribeOn(RLPredicate<Record> filter, ChangeReceiver receiver) {
        Subscriber subs = new Subscriber(filter,receiver);
        this.subscribe(subs);
        return subs;
    }

    default @CallerSideMethod
    Subscriber subscribeOn(String query, ChangeReceiver receiver) throws QParseException {
        Subscriber subs = new Subscriber(new QueryPredicate(query),receiver);
        this.subscribe(subs);
        return subs;
    }

    void unsubscribe( Subscriber subs );

    /**
     * faster than an ordinary query as get is used instead of table scan
     * @param keys
     * @param rec
     * @return
     */
    default @CallerSideMethod Subscriber observe(String[] keys, ChangeReceiver rec) {
        KeySetSubscriber subs = new KeySetSubscriber(keys,rec);
        subscribe(subs);
        return subs;
    }

}

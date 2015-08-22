package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.annotations.CallerSideMethod;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K> {

    void subscribe( Subscriber<K> subs );
    default @CallerSideMethod
    Subscriber<K> subscribeOn(RLPredicate<Record<K>> filter, ChangeReceiver<K> receiver) {
        Subscriber<K> subs = new Subscriber<>(filter,receiver);
        this.subscribe(subs);
        return subs;
    }
    void unsubscribe( Subscriber<K> subs );

}

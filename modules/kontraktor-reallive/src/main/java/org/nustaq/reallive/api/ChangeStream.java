package org.nustaq.reallive.api;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K,V extends Record<K>> {

    default Subscriber<K,V> subscribe( ChangeReceiver<K, V> receiver ) {
        return subscribe( rec -> true, receiver );
    }
    default Subscriber<K,V> subscribe( Predicate<V> filter, ChangeReceiver<K, V> receiver ) {
        Subscriber<K, V> subs = new Subscriber<>(filter, receiver);
        this.subscribe(subs);
        return subs;
    }

    void subscribe( Subscriber<K,V> subs );
    void unsubscribe( Subscriber<K,V> subs );

}

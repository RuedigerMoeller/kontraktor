package org.nustaq.reallive.api;

import org.nustaq.kontraktor.IPromise;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K,V extends Record<K>> {

    void subscribe( Subscriber<K,V> subs );
    void unsubscribe( Subscriber<K,V> subs );

}

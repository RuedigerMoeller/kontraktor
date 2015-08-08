package org.nustaq.reallive.interfaces;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K,V extends Record<K>> {

    void subscribe( Subscriber<K,V> subs );
    void unsubscribe( Subscriber<K,V> subs );

}

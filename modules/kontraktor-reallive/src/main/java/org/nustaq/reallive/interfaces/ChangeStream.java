package org.nustaq.reallive.interfaces;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K> {

    void subscribe( Subscriber<K> subs );
    void unsubscribe( Subscriber<K> subs );

}

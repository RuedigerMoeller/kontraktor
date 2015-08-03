package org.nustaq.reallive.newimpl;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream<K,V extends Record> {

    public void subscribe( Predicate<V> filter, ChangeReceiver rec );

}

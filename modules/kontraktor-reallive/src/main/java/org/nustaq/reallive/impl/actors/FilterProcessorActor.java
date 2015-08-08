package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.*;

/**
 * Created by moelrue on 06.08.2015.
 *
 * Not remoteable, as expects subscriber to already be inThread correctly
 */
@Local public class FilterProcessorActor<K,V extends Record<K>> extends Actor<FilterProcessorActor> implements ChangeReceiver<K,V>, ChangeStream<K,V> {

    FilterProcessor<K,V> filterProcessor;

    public void init( RecordIterable<K,V> iterable ) {
        filterProcessor = new FilterProcessor<>(iterable);
    }

    @Override
    public void receive(ChangeMessage<K, V> change) {
        filterProcessor.receive(change);
    }

    @Override
    public void subscribe(Subscriber<K, V> subs) { // expected to be inThread correctly
        filterProcessor.subscribe(subs);
    }

    @Override
    public void unsubscribe(Subscriber<K, V> subs) {
        filterProcessor.unsubscribe(subs);
    }

}

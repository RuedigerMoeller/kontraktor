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
@Local public class FilterProcessorActor<K> extends Actor<FilterProcessorActor> implements ChangeReceiver<K>, ChangeStream<K> {

    FilterProcessor<K> filterProcessor;

    public void init( RecordIterable<K> iterable ) {
        filterProcessor = new FilterProcessor<>(iterable);
    }

    @Override
    public void receive(ChangeMessage<K> change) {
        filterProcessor.receive(change);
    }

    @Override
    public void subscribe(Subscriber<K> subs) { // expected to be inThread correctly
        filterProcessor.subscribe(subs);
    }

    @Override
    public void unsubscribe(Subscriber<K> subs) {
        filterProcessor.unsubscribe(subs);
    }

}

package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.*;

/**
 * Created by moelrue on 06.08.2015.
 *
 * Not remoteable, as expects subscriber to already be inThread correctly
 *
 * UNSAVE ! if used, tableactor has to queue incoming changes ..
 */
@Local public class FilterProcessorActor<K> extends Actor<FilterProcessorActor> implements FilterProcessor<K> {

    FilterProcessorImpl<K> filterProcessor;

    public void init( RecordIterable<K> iterable, String threadName ) {
        RecordIterable<K> queued = new RecordIterable<K>() {
            @Override
            public <T> void forEach(Spore<Record<K>, T> spore) {
                iterable.filter( rec -> true, (r,e) -> {
                    if ( r != null )
                        spore.remote(r);
                    else if ( e == null )
                        spore.finish();
                    else
                        spore.complete(null,e);
                });
            }
        };
        filterProcessor = new FilterProcessorImpl<>(queued);
        Thread.currentThread().setName(threadName);
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

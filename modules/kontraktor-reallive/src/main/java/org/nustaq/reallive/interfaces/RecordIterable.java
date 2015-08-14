package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.reallive.impl.FilterSpore;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by ruedi on 04/08/15.
 */
public interface RecordIterable<K> {

    <T> void forEach(Spore<Record<K>,T> spore);

    @CallerSideMethod default void filter( RLPredicate<Record<K>> predicate, Callback cb ) {
        forEach(new FilterSpore<>(predicate).forEach(cb).onFinish( () -> cb.finish() ));
    }

}

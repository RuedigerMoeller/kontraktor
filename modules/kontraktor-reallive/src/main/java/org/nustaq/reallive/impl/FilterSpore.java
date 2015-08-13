package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.Record;

import java.util.function.Predicate;

/**
 * Created by ruedi on 13/08/15.
 */
public class FilterSpore<K> extends Spore<Record<K>,Record<K>> {

    Predicate<Record<K>> filter;

    public FilterSpore(Predicate<Record<K>> filter) {
        this.filter = filter;
    }

    @Override
    public void remote(Record<K> input) {
        if ( filter.test(input) ) {
            stream(input);
        }
    }

}

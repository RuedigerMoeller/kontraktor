package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.Record;

/**
 * Created by ruedi on 13/08/15.
 */
public class FilterSpore<K> extends Spore<Record<K>,Record<K>> {

    RLPredicate<Record<K>> filter;

    public FilterSpore(RLPredicate<Record<K>> filter) {
        this.filter = filter;
    }

    @Override
    public void remote(Record<K> input) {
        if ( filter.test(input) ) {
            stream(input);
        }
    }

}

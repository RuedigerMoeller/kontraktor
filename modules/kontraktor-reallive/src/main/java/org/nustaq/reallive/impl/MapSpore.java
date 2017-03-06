package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.RLFunction;
import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.Record;

/**
 * Created by ruedi on 06.03.17.
 *
 * should be option of filterspore, but cannot because of fixed types ..
 */
public class MapSpore<K,V> extends Spore<Record<K>,V> {

    private final RLFunction<Record<K>, V> mapFun;
    private final RLPredicate<Record<K>> filter; // may modify record (gets patchable private copy

    public MapSpore(RLPredicate<Record<K>> filter, RLFunction<Record<K>,V> mapFun ) {
        this.filter = filter;
        this.mapFun = mapFun;
    }

    @Override
    public void remote(Record<K> input) {
        if ( filter != null && ! filter.test(input) ) {
            return;
        }
        stream( mapFun.apply(input) );
    }

}

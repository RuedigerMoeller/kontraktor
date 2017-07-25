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
public class MapSpore<K,V> extends Spore<Record,V> {

    private final RLFunction<Record, V> mapFun;
    private final RLPredicate<Record> filter; // may modify record (gets patchable private copy

    public MapSpore(RLPredicate<Record> filter, RLFunction<Record,V> mapFun ) {
        this.filter = filter;
        this.mapFun = mapFun;
    }

    @Override
    public void remote(Record input) {
        if ( filter != null && ! filter.test(input) ) {
            return;
        }
        stream( mapFun.apply(input) );
    }

}

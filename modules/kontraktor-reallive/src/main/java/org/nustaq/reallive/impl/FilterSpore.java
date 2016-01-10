package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.records.PatchingRecord;

/**
 * Created by ruedi on 13/08/15.
 */
public class FilterSpore<K> extends Spore<Record<K>,Record<K>> {

    RLPredicate<Record<K>> filter;

    public FilterSpore(RLPredicate<Record<K>> filter) {
        this.filter = filter;
    }

    transient PatchingRecord rec;
    @Override
    public void remote(Record<K> input) {
        if ( rec == null ) {
            rec = new PatchingRecord(input);
        } else {
            rec.reset(input);
        }
        if ( filter.test(rec) ) {
            stream(rec.unwrapOrCopy());
        }
    }

}

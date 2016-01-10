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

    transient static ThreadLocal<PatchingRecord> rec = new ThreadLocal<PatchingRecord>() {
        @Override
        protected PatchingRecord initialValue() {
            return new PatchingRecord(null);
        }
    };

    @Override
    public void remote(Record<K> input) {
        final PatchingRecord patchingRecord = rec.get();
        patchingRecord.reset(input);
        if ( filter.test(patchingRecord) ) {
            stream(patchingRecord.unwrapOrCopy());
        }
    }

}

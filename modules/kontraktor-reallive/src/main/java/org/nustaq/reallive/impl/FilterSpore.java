package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.records.PatchingRecord;

/**
 * Created by ruedi on 13/08/15.
 */
public class FilterSpore<K> extends Spore<Record<K>,Record<K>> {

    RLPredicate<Record<K>> filter; // may modify record (gets patchable private copy
    RLPredicate<Record<K>> prePatchFilter; // gets original record (no modification)

    public FilterSpore(RLPredicate<Record<K>> filter) {
        this(filter,null);
    }
    public FilterSpore(RLPredicate<Record<K>> filter,RLPredicate<Record<K>> prePatchFilter) {
        this.filter = filter;
        this.prePatchFilter = prePatchFilter;
    }

    public transient static ThreadLocal<PatchingRecord> rec = new ThreadLocal<PatchingRecord>() {
        @Override
        protected PatchingRecord initialValue() {
            return new PatchingRecord(null);
        }
    };

    @Override
    public void remote(Record<K> input) {
        if ( prePatchFilter != null && ! prePatchFilter.test(input) ) {
            return;
        }

        final PatchingRecord patchingRecord = rec.get();
        patchingRecord.reset(input);
        if ( filter.test(patchingRecord) ) {
            stream(patchingRecord.unwrapOrCopy());
        }
    }

}

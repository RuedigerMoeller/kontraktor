package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.RLPredicate;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.PatchingRecord;

/**
 * Created by ruedi on 13/08/15.
 */
public class FilterSpore extends Spore<Record,Record> {

    RLPredicate<Record> filter; // may modify record (gets patchable private copy
    boolean modifiesResult;

    public FilterSpore(RLPredicate<Record> filter) {
        this.filter = filter;
    }

    public transient static ThreadLocal<PatchingRecord> rec = new ThreadLocal<PatchingRecord>() {
        @Override
        protected PatchingRecord initialValue() {
            return new PatchingRecord(null);
        }
    };

    public RLPredicate<Record> getFilter() {
        return filter;
    }

    @Override
    public void remote(Record input) {
        if (modifiesResult) {
            final PatchingRecord patchingRecord = rec.get();
            patchingRecord.reset(input);
            if (filter.test(patchingRecord)) {
                stream(patchingRecord.unwrapOrCopy());
            }
        } else {
            if (filter.test(input)) {
                stream(input);
            }
        }
    }

    public FilterSpore modifiesResult(boolean modifiesResult) {
        this.modifiesResult = modifiesResult;
        return this;
    }

}

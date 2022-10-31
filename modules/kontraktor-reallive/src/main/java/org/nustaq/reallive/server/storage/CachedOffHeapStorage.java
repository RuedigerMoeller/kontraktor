package org.nustaq.reallive.server.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.server.RemoveLog;
import org.nustaq.reallive.server.StorageDriver;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.records.MapRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by ruedi on 08/12/15.
 */
public class CachedOffHeapStorage implements RecordStorage {

    RecordPersistance persisted;
    HeapRecordStorage onHeap;

    public CachedOffHeapStorage(RecordPersistance persisted, HeapRecordStorage onHeap) {
        this.persisted = persisted;
        this.onHeap = onHeap;
        bootstrapCache(persisted, onHeap);
    }

    protected void bootstrapCache(RecordPersistance persisted, HeapRecordStorage onHeap) {
        List<Record> reput = new ArrayList();
        // note: this runs local no remote spore is applied
        persisted.forEachWithSpore(new Spore<Record, Object>() {
            boolean hadErr = false;
            @Override
            public void remote(Record input) {
                try {
                    Record finalRec = StorageDriver.unwrap(input);
                    if (finalRec != input) {
                        reput.add(finalRec);
                    }
                    // record class conversion
                    if ( MapRecord.conversion != null && finalRec.getClass() != MapRecord.recordClass) {
                        finalRec = MapRecord.conversion.apply((MapRecord) finalRec);
                        reput.add(finalRec);
                    }
                    if ( finalRec._afterLoad() ) {
                        reput.add(finalRec);
                    }
                    onHeap._rawPut(input.getKey(), finalRec);
                } catch (Exception e) {
                    if ( hadErr )
                        Log.Error(this, "no trace (repetition): "+e);
                    else {
                        hadErr = true;
                        Log.Error(this,e);
                    }
                }
            }
        }.onFinish( () -> {
            for (int i = 0; i < reput.size(); i++) {
                Record record = reput.get(i);
                persisted._rawPut((String) record.getKey(),record);
            }
        }));
    }

    @Override
    public RecordStorage put(String key, Record value) {
        value.internal_updateLastModified();
        return _rawPut(key,value);
    }

    @Override
    public RecordStorage _rawPut(String key, Record value) {
        persisted._rawPut(key,value);
        onHeap._rawPut(key,value);
        return this;
    }
    @Override
    public Record get(String key) {
        return onHeap.get(key);
    }

    @Override
    public Record remove(String key) {
        persisted.remove(key);
        Record res = onHeap.remove(key);
        return res;
    }

    @Override
    public long size() {
        return onHeap.size();
    }

    @Override
    public StorageStats getStats() {
        return persisted.getStats();
    }

    @Override
    public Stream<Record> stream() {
        return onHeap.stream();
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        persisted.resizeIfLoadFactorLarger(loadFactor,maxGrowBytes);
    }

    @Override
    public RemoveLog getRemoveLog() {
        return persisted.getRemoveLog();
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        onHeap.forEachWithSpore(spore);
    }

    // can be empty op for cached/inmem storage
    public void _saveMapping(ClusterTableRecordMapping mapping) {
        persisted._saveMapping(mapping);
    }
    public ClusterTableRecordMapping _loadMapping() {
        return persisted._loadMapping();
    }

}

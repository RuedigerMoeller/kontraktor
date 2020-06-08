package org.nustaq.reallive.server.storage;

import org.nustaq.kontraktor.Spore;
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
        List<Record> reput = new ArrayList();
        persisted.forEachWithSpore(new Spore<Record, Object>() {
            @Override
            public void remote(Record input) {
                Record unwrap = StorageDriver.unwrap(input);
                if ( unwrap != input ) {
                    reput.add(unwrap);
                }
                if ( unwrap.getClass() != MapRecord.recordClass && MapRecord.conversion != null ) {
                    unwrap = MapRecord.conversion.apply((MapRecord) unwrap);
                    reput.add(unwrap);
                }
                onHeap._put(input.getKey(), unwrap);
            }
        }.onFinish( () -> {
            for (int i = 0; i < reput.size(); i++) {
                Record record = reput.get(i);
                persisted._put((String) record.getKey(),record);
            }
        }));
    }

    @Override
    public RecordStorage put(String key, Record value) {
        value.internal_updateLastModified();
        persisted._put(key,value);
        onHeap._put(key,value);
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

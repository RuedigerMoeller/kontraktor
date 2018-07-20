package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.impl.StorageDriver;
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

    OffHeapRecordStorage offheap;
    HeapRecordStorage onHeap;

    public CachedOffHeapStorage(OffHeapRecordStorage offheap, HeapRecordStorage onHeap) {
        this.offheap = offheap;
        this.onHeap = onHeap;
        List<Record> reput = new ArrayList();
        offheap.forEachWithSpore(new Spore<Record, Object>() {
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
                onHeap.put(input.getKey(), unwrap);
            }
        });
        for (int i = 0; i < reput.size(); i++) {
            Record record = reput.get(i);
            offheap.put((String) record.getKey(),record);
        }
    }

    @Override
    public RecordStorage put(String key, Record value) {
        value.updateLastModified();
        offheap._put(key,value);
        onHeap._put(key,value);
        return this;
    }

    @Override
    public Record get(String key) {
        return onHeap.get(key);
    }

    @Override
    public Record remove(String key) {
        Record res = offheap.remove(key);
        onHeap.remove(key);
        return res;
    }

    @Override
    public long size() {
        return onHeap.size();
    }

    @Override
    public StorageStats getStats() {
        return offheap.getStats();
    }

    @Override
    public Stream<Record> stream() {
        return onHeap.stream();
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        offheap.resizeIfLoadFactorLarger(loadFactor,maxGrowBytes);
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        onHeap.forEachWithSpore(spore);
    }

}

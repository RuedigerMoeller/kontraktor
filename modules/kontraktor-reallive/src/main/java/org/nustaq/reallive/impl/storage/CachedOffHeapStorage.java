package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.impl.StorageDriver;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.RecordStorage;
import org.nustaq.reallive.records.MapRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by ruedi on 08/12/15.
 */
public class CachedOffHeapStorage implements RecordStorage<String> {

    OffHeapRecordStorage offheap;
    HeapRecordStorage<String> onHeap;

    public CachedOffHeapStorage(OffHeapRecordStorage offheap, HeapRecordStorage<String> onHeap) {
        this.offheap = offheap;
        this.onHeap = onHeap;
        List<Record> reput = new ArrayList<>();
        offheap.forEach(new Spore<Record<String>, Object>() {
            @Override
            public void remote(Record<String> input) {
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
    public RecordStorage put(String key, Record<String> value) {
        offheap.put(key,value);
        onHeap.put(key,value);
        return this;
    }

    @Override
    public Record<String> get(String key) {
        return onHeap.get(key);
    }

    @Override
    public Record<String> remove(String key) {
        Record<String> res = offheap.remove(key);
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
    public Stream<Record<String>> stream() {
        return onHeap.stream();
    }

    @Override
    public <T> void forEach(Spore<Record<String>, T> spore) {
        onHeap.forEach(spore);
    }

}

package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.RecordStorage;

/**
 * Created by ruedi on 08/12/15.
 */
public class CachedOffHeapStorage implements RecordStorage<String> {

    OffHeapRecordStorage offheap;
    HeapRecordStorage<String> onHeap;

    public CachedOffHeapStorage(OffHeapRecordStorage offheap, HeapRecordStorage<String> onHeap) {
        this.offheap = offheap;
        this.onHeap = onHeap;
        offheap.forEach(new Spore<Record<String>, Object>() {
            @Override
            public void remote(Record<String> input) {
                onHeap.put(input.getKey(), input);
            }
        });
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
    public <T> void forEach(Spore<Record<String>, T> spore) {
        onHeap.forEach(spore);
    }

}

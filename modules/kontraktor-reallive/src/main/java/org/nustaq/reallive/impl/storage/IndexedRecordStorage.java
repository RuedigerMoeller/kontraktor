package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.api.StorageIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class IndexedRecordStorage implements RecordStorage {
    RecordStorage wrapped;
    List<StorageIndex> indices;

    @Override
    public RecordStorage put(String key, Record value) {
        if ( indices != null ) {
            indices.forEach( ind -> ind.put(key,value) );
        }
        return wrapped.put(key,value);
    }

    @Override
    public Record get(String key) {
        return wrapped.get(key);
    }

    @Override
    public Record remove(String key) {
        if ( indices != null ) {
            indices.forEach( ind -> ind.remove(key) );
        }
        return wrapped.remove(key);
    }

    @Override
    public long size() {
        return wrapped.size();
    }

    @Override
    public StorageStats getStats() {
        return wrapped.getStats();
    }

    @Override
    public Stream<Record> stream() {
        return wrapped.stream();
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrow) {
        wrapped.resizeIfLoadFactorLarger(loadFactor,maxGrow);
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        wrapped.forEachWithSpore(spore);
    }
}

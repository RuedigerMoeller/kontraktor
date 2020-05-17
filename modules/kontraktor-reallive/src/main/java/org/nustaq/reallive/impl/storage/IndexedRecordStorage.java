package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.api.StorageIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class IndexedRecordStorage implements RecordStorage {
    RecordStorage wrapped;
    List<StorageIndex> indices = new ArrayList();
    Map<String,HashIndex> hashIndizes = new HashMap<>();

    public IndexedRecordStorage wrapped(RecordStorage r) {
        wrapped = r;
        return this;
    }

    public RecordStorage getWrapped() {
        return wrapped;
    }

    public List<StorageIndex> getIndices() {
        return indices;
    }

    public void addIndex(StorageIndex s) {
        indices.add(s);
        if ( s instanceof HashIndex )
            hashIndizes.put(((HashIndex) s).getHashPath(), (HashIndex) s);
    }

    @Override
    public RecordStorage put(String key, Record value) {
        if ( indices != null ) {
            indices.forEach( ind -> ind.put(value.getKey(),value) );
        }
        return wrapped.put(key,value);
    }

    public void initializeFromRecord(Record value) {
        if ( indices != null ) {
            indices.forEach( ind -> ind.put(value.getKey(),value) );
        }
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

    public HashIndex getHashIndex(String path) {
        return hashIndizes.get(path);
    }
}

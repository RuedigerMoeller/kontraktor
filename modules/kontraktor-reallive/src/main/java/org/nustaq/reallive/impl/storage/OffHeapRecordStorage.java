package org.nustaq.reallive.impl.storage;

import org.nustaq.offheap.FSTBinaryOffheapMap;
import org.nustaq.offheap.FSTSerializedOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.nustaq.serialization.simpleapi.FSTCoder;
import org.nustaq.serialization.util.FSTUtil;

import java.util.function.*;

/**
 * Created by moelrue on 05.08.2015.
 */
public class OffHeapRecordStorage<V extends Record<String>> implements RecordStorage<String,V> {

    FSTCoder coder;

    FSTSerializedOffheapMap<String,V> store;
    int keyLen;

    protected OffHeapRecordStorage() {}

    public OffHeapRecordStorage(int maxKeyLen, int sizeMB, int estimatedNumRecords) {
        keyLen = maxKeyLen;
        init(null, sizeMB, estimatedNumRecords, maxKeyLen, false, (Class[])null ); //Record.class);
    }

    protected void init(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen, boolean persist, Class... toReg) {
        this.keyLen = keyLen;
        coder = new DefaultCoder();
        if ( toReg != null )
            coder.getConf().registerClass(toReg);
        if ( persist ) {
            try {
                store = createPersistentMap(tableFile, sizeMB, estimatedNumRecords, keyLen);
            } catch (Exception e) {
                FSTUtil.rethrow(e);
            }
        }
        else
            store = createMemMap(sizeMB, estimatedNumRecords, keyLen);
    }

    protected FSTSerializedOffheapMap<String,V> createMemMap(int sizeMB, int estimatedNumRecords, int keyLen) {
        return new FSTUTFStringOffheapMap(keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords, coder);
    }

    protected FSTSerializedOffheapMap<String,V> createPersistentMap(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen) throws Exception {
        return new FSTUTFStringOffheapMap<>(tableFile, keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords, coder);
    }

    @Override
    public RecordStorage put(String key, V value) {
        store.put(key,value);
        return this;
    }

    Thread _t;
    void checkThread() {
        if ( _t == null ) {
            _t = Thread.currentThread();
        } else if ( _t != Thread.currentThread() ){
            throw new RuntimeException("Unexpected MultiThreading");
        }
    }

    @Override
    public V get(String key) {
        checkThread();
        return store.get(key);
    }

    @Override
    public V remove(String key) {
        V v = get(key);
        if ( v != null )
            store.remove(key);
        return v;
    }

    @Override
    public long size() {
        return store.getSize();
    }

    @Override
    public void forEach(Predicate<V> filter, Consumer<V> action) {
        store.values().forEachRemaining(record -> {
            if (filter.test(record)) {
                action.accept(record);
            }
        });
    }
}

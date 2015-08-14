package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.offheap.FSTBinaryOffheapMap;
import org.nustaq.offheap.FSTSerializedOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.nustaq.serialization.simpleapi.FSTCoder;
import org.nustaq.serialization.util.FSTUtil;

import java.util.Iterator;

/**
 * Created by moelrue on 05.08.2015.
 */
public class OffHeapRecordStorage implements RecordStorage<String> {

    FSTCoder coder;

    FSTSerializedOffheapMap<String,Record<String>> store;
    int keyLen;

    protected OffHeapRecordStorage() {}

    public OffHeapRecordStorage(int maxKeyLen, int sizeMB, int estimatedNumRecords) {
        keyLen = maxKeyLen;
        init(null, sizeMB, estimatedNumRecords, maxKeyLen, false, Record.class );
    }

    public OffHeapRecordStorage(String file, int maxKeyLen, int sizeMB, int estimatedNumRecords) {
        keyLen = maxKeyLen;
        init(file, sizeMB, estimatedNumRecords, maxKeyLen, false, Record.class );
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

    protected FSTSerializedOffheapMap<String,Record<String>> createMemMap(int sizeMB, int estimatedNumRecords, int keyLen) {
        return new FSTUTFStringOffheapMap(keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords, coder);
    }

    protected FSTSerializedOffheapMap<String,Record<String>> createPersistentMap(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen) throws Exception {
        return new FSTUTFStringOffheapMap<>(tableFile, keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords, coder);
    }

    @Override
    public RecordStorage put(String key, Record<String> value) {
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
    public Record<String> get(String key) {
        checkThread();
        return store.get(key);
    }

    @Override
    public Record<String> remove(String key) {
        Record<String> v = get(key);
        if ( v != null )
            store.remove(key);
        return v;
    }

    @Override
    public long size() {
        return store.getSize();
    }

    @Override
    public <T> void forEach(Spore<Record<String>, T> spore) {
        for (Iterator iterator = store.values(); iterator.hasNext(); ) {
            Record<String> record = (Record<String>) iterator.next();
            spore.remote(record);
            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }
}

package org.nustaq.reallive.impl.storage;

import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTBinaryOffheapMap;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.offheap.bytez.Bytez;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.nustaq.serialization.simpleapi.FSTCoder;

import java.util.Iterator;

/**
 * Created by ruedi on 02.07.14.
 */
public class FSTBinaryStorage<V> implements BinaryStorage<String,V> {

    FSTAsciiStringOffheapMap<V> store;
    int keyLen;
    FSTCoder conf;

    public FSTBinaryStorage() {
    }

    public void init(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen, boolean persist, Class... toReg) throws Exception {
        this.keyLen = keyLen;
        conf = new DefaultCoder();
        conf.getConf().registerClass(toReg);
        if ( persist )
            store = new FSTAsciiStringOffheapMap<>(tableFile, keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords,conf);
        else
            store = new FSTAsciiStringOffheapMap<>(keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords,conf);
    }

    @Override
    public void put(String key, V toWrite) {
        store.put(key,toWrite);
    }

    @Override
    public V get(String key) {
        return store.get(key);
    }

    @Override
    public Iterator<ByteSource> binaryValues() {
        return store.binaryValues();
    }

    @Override
    public Iterator<V> values() {
        return store.values();
    }

    @Override
    public V decodeValue(ByteSource value) {
        return store.decodeValue((org.nustaq.offheap.bytez.bytesource.BytezByteSource) value);
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    @Override
    public V removeAndGet(String key) {
        V res = store.get(key);
        if ( res != null )
            store.remove(key);
        return res;
    }

    @Override
    public Bytez getCustomStorage() {
        return store.getCustomFileHeader();
    }

    @Override
    public int size() {
        return store.getSize();
    }

    @Override
    public int getKeyLen() {
        return keyLen;
    }

    @Override
    public int getFreeMB() {
        return store.getCapacityMB()-(int)(store.getUsedMem()/1024/1024);
    }

    @Override
    public int getSizeMB() {
        return store.getCapacityMB();
    }

    @Override
    public boolean contains(String key) {
        return store.getBinary(store.encodeKey(key)) != null;
    }

    @Override
    public void close() {
        store.free();
    }

}

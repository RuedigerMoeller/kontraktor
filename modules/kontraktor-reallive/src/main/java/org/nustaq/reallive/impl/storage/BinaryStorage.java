package org.nustaq.reallive.impl.storage;

import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.offheap.bytez.Bytez;

import java.util.Iterator;

/**
 * Created by ruedi on 21.06.14.
 */
public interface BinaryStorage<K,V> {

    public void put(K key, V toWrite );
    public V get(K key);
    public Iterator<ByteSource> binaryValues();
    public Iterator<V> values();

    public V decodeValue(ByteSource value);

    public void remove(K key);
    public V removeAndGet(K key);

    public Bytez getCustomStorage();

    int size();
    int getKeyLen();

    int getFreeMB();
    int getSizeMB();

    boolean contains(String key);

    void close();
}

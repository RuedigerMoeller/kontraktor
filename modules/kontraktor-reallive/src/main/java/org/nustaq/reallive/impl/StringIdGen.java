package org.nustaq.reallive.impl;

import org.nustaq.offheap.bytez.Bytez;
import org.nustaq.reallive.impl.storage.BinaryStorage;

/**
 * Threadsafe String id generator.
 */
public class StringIdGen implements IdGenerator<String> {

    String prefix;
    BinaryStorage storage;

    public StringIdGen(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String nextid() {
        Bytez state = storage.getCustomStorage();
        long count = state.getLong(0);
        while( ! state.compareAndSwapLong(0, count, count +1) ) {
            count = state.getLong(0);
        }
        return prefix+Long.toHexString(count);
    }

    @Override
    public int setState(BinaryStorage storage) {
        Bytez bytes = storage.getCustomStorage();
        long aLong = bytes.getLong(0);
        if ( aLong == 0 ) // new file
            bytes.putLong(0,1);
        this.storage = storage;
        return 8;
    }

}

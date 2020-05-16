package org.nustaq.reallive.api;

import org.nustaq.kontraktor.Spore;

public interface StorageIndex {
    void put(String key, Record value);

    void remove(String key);

    <T> void forEachWithSpore(Object hashValue, Spore<Record, T> spore, RecordStorage store);
}

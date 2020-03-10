package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.Record;

/**
 * interface can be used in conjunction with CachedRecordStorage to provide a persistance layer
 */
public interface RecordPersistance {

    Record remove(String key);

    StorageStats getStats();

    default void resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        // do nothing, special for built in persistence layer
    }

    <T> void forEachWithSpore(Spore<Record, T> spore);

    RecordPersistance put(String key, Record record);

    default RecordPersistance _put(String key, Record value) {
        throw new RuntimeException("not usable as persistance implementation");
    }

}

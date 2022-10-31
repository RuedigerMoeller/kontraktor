package org.nustaq.reallive.server.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.server.RemoveLog;

/**
 * interface can be used in conjunction with CachedRecordStorage to provide a persistance layer
 */
public interface RecordPersistance {

    Record remove(String key); // SOME IMPLEMENTATIONS ALWAYS RETURN NULL (method signature trouble)

    StorageStats getStats();

    default void resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        // do nothing, special for built in persistence layer
    }

    <T> void forEachWithSpore(Spore<Record, T> spore);

    RecordPersistance put(String key, Record record);

    /**
     * does not update lastmodified timestamp
     *
     * @param key
     * @param value
     * @return
     */
    default RecordPersistance _rawPut(String key, Record value) {
        throw new RuntimeException("not usable as persistance implementation");
    }

    // can be empty op for cached/inmem storage
    default void _saveMapping(ClusterTableRecordMapping mapping) {}
    default ClusterTableRecordMapping _loadMapping() {
        return null;
    }

    RemoveLog getRemoveLog();

}

package org.nustaq.reallive.api;


import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import org.nustaq.reallive.server.storage.RecordPersistance;
import org.nustaq.reallive.server.storage.StorageStats;

import java.util.stream.Stream;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStorage extends RealLiveStreamActor, RecordPersistance {

    RecordStorage put(String key, Record value );
    Record get(String key );
    Record remove(String key );
    long size();
    StorageStats getStats();
    Stream<Record> stream();
    // administration level method
    void resizeIfLoadFactorLarger( double loadFactor, long maxGrow );

}

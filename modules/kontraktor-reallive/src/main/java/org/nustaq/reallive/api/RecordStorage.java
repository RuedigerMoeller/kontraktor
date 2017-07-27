package org.nustaq.reallive.api;


import org.nustaq.reallive.impl.storage.StorageStats;

import java.util.stream.Stream;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStorage extends RealLiveStreamActor {

    RecordStorage put(String key, Record value );
    Record get(String key );
    Record remove(String key );
    long size();
    StorageStats getStats();
    Stream<Record> stream();
    // administration level method
    void resizeIfLoadFactorLarger( double loadFactor, long maxGrow );
}

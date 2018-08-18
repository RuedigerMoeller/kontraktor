package org.nustaq.reallive.api;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.impl.storage.StorageStats;

/**
 * contains only methods without lambdas (avoids issues caused by lambda version changes).
 * distributed lambda execution requires all nodes to have the full application code on the
 * classpath. By restricting to non lambda api, its possible to operate the datagrid independent
 * off client versions.
 */
public interface SafeRealLiveTable extends ChangeReceiver, SafeChangeStream, SafeRealLiveStreamActor {

    IPromise<Object> ping();
    IPromise<TableDescription> getDescription();
    void stop();
    IPromise<StorageStats> getStats();
    IPromise<Long> size();
    // administrative .. avoid growing at an arbitrary point in time
    IPromise resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes);
    IPromise<Record> get(String key);

    void put(String key, Object... keyVals);
    void merge(String key, Object... keyVals);
    IPromise<Boolean> add(String key, Object ... keyVals );
    void update(String key, Object ... keyVals );
    IPromise<Record> take(String key);
    void remove(String key);

    void mergeRecord(Record rec);
    void setRecord(Record rec);
    // named 'set' to avoid calling put(key,Record) actualle invoking put(key, ...keyval) accidentally
    IPromise<Boolean> addRecord(Record rec );


}

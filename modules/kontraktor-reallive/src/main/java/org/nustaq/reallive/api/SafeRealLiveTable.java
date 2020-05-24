package org.nustaq.reallive.api;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.server.storage.StorageStats;

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

    void put(int senderId, String key, Object... keyVals);
    void merge(int senderId, String key, Object... keyVals);
    IPromise<Boolean> add(int senderId, String key, Object... keyVals);
    void update(int senderId, String key, Object... keyVals);
    IPromise<Record> take(int senderId, String key);
    void remove(int senderId, String key);

    void mergeRecord(int senderId, Record rec);
    void setRecord(int senderId, Record rec);
    IPromise<Boolean> addRecord(int sederId, Record rec);

    @CallerSideMethod default void put(String key, Object... keyVals) {
        this.put(0,key,keyVals);
    }
    @CallerSideMethod default void merge(String key, Object... keyVals) {
        this.merge(0,key,keyVals);
    }
    @CallerSideMethod default IPromise<Boolean> add(String key, Object... keyVals) {
        return this.add(0,key,keyVals);
    }
    @CallerSideMethod default void update(String key, Object... keyVals) {
        this.update(0,key,keyVals);
    }
    @CallerSideMethod default void remove(String key) {
        this.remove(0,key);
    }

    @CallerSideMethod default void mergeRecord(Record rec) {
        this.mergeRecord(0,rec);
    }
    @CallerSideMethod default void setRecord(Record rec) {
        this.setRecord(0,rec);
    }

    // named 'set' to avoid calling put(key,Record) actualle invoking put(key, ...keyval) accidentally
    @CallerSideMethod default IPromise<Boolean> addRecord(Record rec) {
        return this.addRecord(0,rec);
    }

}

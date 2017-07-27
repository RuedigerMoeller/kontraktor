package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.impl.storage.StorageStats;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable extends ChangeReceiver, RealLiveStreamActor, ChangeStream, AsyncKV, Mutatable<Object> {

    IPromise ping();
    IPromise<TableDescription> getDescription();
    void stop();
    IPromise<StorageStats> getStats();

    IPromise<Boolean> putCAS(RLPredicate<Record> casCondition, String key, Object[] keyVals);
    void atomic(String key, RLConsumer<Record> action);
    IPromise atomicQuery(String key, RLFunction<Record, Object> action);

    /**
     * @param filter
     * @param action - return true in order to update record, false in order to remove the record
     */
    void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record,Boolean> action);

    // administrative .. avoid growing at an arbitrary point in time
    IPromise resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes);


}

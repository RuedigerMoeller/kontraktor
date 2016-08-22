package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.impl.storage.StorageStats;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable<K> extends ChangeReceiver<K>, RealLiveStreamActor<K>, ChangeStream<K>, AsyncKV<K>, Mutatable<K> {

    IPromise ping();
    IPromise<TableDescription> getDescription();
    void stop();
    IPromise<StorageStats> getStats();

    IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object[] keyVals);
    void atomic(K key, RLConsumer<Record<K>> action);
    IPromise atomicQuery(K key, RLFunction<Record<K>, Object> action);
    void atomicUpdate(RLPredicate<Record<K>> filter, RLFunction<Record<K>,Boolean> action);

}

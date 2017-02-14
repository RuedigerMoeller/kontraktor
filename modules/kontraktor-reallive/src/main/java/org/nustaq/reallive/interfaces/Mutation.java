package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

import java.util.function.Consumer;

/**
 * Created by moelrue on 05.08.2015.
 */
public interface Mutation<K> {

    /**
     * FIXME: does not trigger a change notification !
     * @param casCondition
     * @param key
     * @param keyVals
     * @return
     */
    IPromise<Boolean> putCAS( RLPredicate<Record<K>> casCondition, K key, Object... keyVals);
    void atomic(K key, RLConsumer action);

    /**
     * apply the function to the record with given key and return the result inside a promise
     *
     * changes to the record inside the function are applied to the real record and a change message
     * is generated.
     *
     * In case the function returns a changemessage (add,put,remove ..), the change message is applied
     * to the original record and broadcasted
     *
     * @param key
     * @param action
     * @return the result of function.
     */
    IPromise atomicQuery(K key, RLFunction<Record<K>,Object> action);

    /**
     *
     * @param filter - selects record
     * @param action - function, the function might modify the record using putField. If false is returned, the record is deleted
     */
    void atomicUpdate(RLPredicate<Record<K>> filter, RLFunction<Record<K>, Boolean> action);

    // FIXME: collides with put key, record
    void put(K key, Object... keyVals);
    void addOrUpdate(K key, Object... keyVals);
    void add( K key, Object ... keyVals );
    void add( Record<K> rec );
    void addOrUpdateRec(Record<K> rec);
    void put(Record<K> rec);
    void update( K key, Object ... keyVals );
    void remove(K key);

}

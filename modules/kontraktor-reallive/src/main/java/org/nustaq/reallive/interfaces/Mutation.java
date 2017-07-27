package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by moelrue on 05.08.2015.
 */
public interface Mutation {

    /**
     * @param casCondition
     * @param key
     * @param keyVals
     * @return
     */
    IPromise<Boolean> putCAS(RLPredicate<Record> casCondition, String key, Object... keyVals);
    void atomic(String key, RLConsumer action);

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
    IPromise atomicQuery(String key, RLFunction<Record,Object> action);

    /**
     *
     * @param filter - selects record
     * @param action - function, the function might modify the record using putField. If false is returned, the record is deleted
     */
    void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action);

    // FIXME: collides with put key, record
    void put(String key, Object... keyVals);
    void addOrUpdate(String key, Object... keyVals);
    void add(String key, Object ... keyVals );
    void add( Record rec );
    void addOrUpdateRec(Record rec);
    void put(Record rec);
    void update(String key, Object ... keyVals );
    void remove(String key);

}

package org.nustaq.reallive.api;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable extends SafeRealLiveTable, ChangeStream, RealLiveStreamActor {

    /**
     * apply the function to the record with given key and return the result inside a promise
     *
     * changes to the record inside the function are applied to the real record and a change message
     * is generated.
     *
     * In case the function returns a changemessage (add,putRecord,remove ..), the change message is applied
     * to the original record and broadcasted
     *
     * @param key
     * @param action
     * @return the result of function.
     */
    IPromise atomicQuery(String key, RLFunction<Record,Object> action);

    /**
     * mass update.
     *
     * @param filter - selects records
     * @param action - function, the function might modify the record using putField. If false is returned, the record is deleted
     */
    void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action);

}

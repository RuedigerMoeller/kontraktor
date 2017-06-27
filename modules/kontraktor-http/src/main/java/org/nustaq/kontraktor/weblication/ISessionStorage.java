package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.IPromise;
import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by ruedi on 20.06.17.
 *
 */
public interface ISessionStorage {

    enum Action {
        PUT,
        DELETE
    }

    class AtomicResult implements Serializable {
        Action action;
        Object record;
        Object returnValue;

        public Action getAction() {
            return action;
        }

        public Object getRecord() {
            return record;
        }

        public AtomicResult action(final Action action) {
            this.action = action;
            return this;
        }

        public AtomicResult record(final Object record) {
            this.record = record;
            return this;
        }

        public AtomicResult returnValue(final Object returnValue) {
            this.returnValue = returnValue;
            return this;
        }

        public Object getReturnValue() {
            return returnValue;
        }
    }

    IPromise<String> getUserFromSessionId(String sid);
    IPromise atomic(String userId, Function<Object, AtomicResult> recordConsumer);
    void storeUserRecord(String userId, Object userRecord);
    IPromise storeIfNotPresent(String userId, Object userRecord);
    IPromise getUserRecord(String userId);

}

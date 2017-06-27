package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.IPromise;
import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by ruedi on 20.06.17.
 */
public interface ISessionStorage {

    enum Action {
        PUT,
        DELETE
    }

    class AtomicResult implements Serializable {
        Action action;
        Object record;

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

    }

    IPromise<String> getUserFromSessionId(String sid);

    IPromise<AtomicResult> atomic(String userId, Function<Object,Action> recordConsumer);
    void storeUserRecord(String userId, Object userRecord);
    IPromise storeIfNotPresent(String userId, Object userRecord);

    IPromise getUserRecord(String userId);

}

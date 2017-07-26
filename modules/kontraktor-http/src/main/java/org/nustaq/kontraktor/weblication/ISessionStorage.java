package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.Callback;
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
        PersistedRecord record;
        Object returnValue;

        public Action getAction() {
            return action;
        }

        public PersistedRecord getRecord() {
            return record;
        }

        public AtomicResult action(final Action action) {
            this.action = action;
            return this;
        }

        public AtomicResult record(final PersistedRecord record) {
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

    IPromise<String> getUserKeyFromSessionId(String sid);
    void putSessionId(String sessionId, String user);

    /**
     * fixme: too complicated
     */
    IPromise atomic(String key, Function<PersistedRecord, AtomicResult> fun);
    void storeRecord(PersistedRecord userRecord);
    void delRecord(String userkey);
    IPromise<Boolean> storeIfNotPresent(PersistedRecord userRecord);
    IPromise<PersistedRecord> getUserRecord(String userId);

    /**
     * stream all user records to the given callback and close it calling cb.finish()
     * @param cb
     */
    void forEach(Callback<PersistedRecord> cb);
}

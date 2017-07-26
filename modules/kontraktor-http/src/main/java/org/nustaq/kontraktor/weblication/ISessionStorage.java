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

    IPromise<String> getUserFromSessionId(String sid);
    void putUserAtSessionId(String sessionId, String userKey);

    void storeRecord(PersistedRecord userRecord);
    void delRecord(String userkey);
    IPromise<Boolean> storeIfNotPresent(PersistedRecord userRecord);
    IPromise<PersistedRecord> getUserRecord(String userId);

    /**
     * stream all user records to the given callback and close it calling cb.finish()
     * @param cb
     */
    void forEachUser(Callback<PersistedRecord> cb);
}

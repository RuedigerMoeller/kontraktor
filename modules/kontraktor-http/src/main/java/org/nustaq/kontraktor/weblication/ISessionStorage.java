package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.weblication.model.PersistedRecord;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by ruedi on 20.06.17.
 *
 * Defines Persistance requirements. There is a default implementation based on memory mapped
 * files which should be sufficient for small to medium sized apps (<0.5 million user).
 *
 * The interface could be implemented backed by your favourite data base or data grid.
 */
public interface ISessionStorage {

    class Token implements Serializable {
        String userId;
        String data;
        long lifeTime;

        public Token(String userId, String data, long lifeTime) {
            this.userId = userId;
            this.data = data;
            this.lifeTime = lifeTime;
        }

        public String getUserId() {
            return userId;
        }

        public String getData() {
            return data;
        }

        public long getLifeTime() {
            return lifeTime;
        }
    }

    /**
     * creates a persisted token associated with the user and data. (e.g. a pending confirmation email)
     *
     * @return a unique string identifier
     */
    IPromise<String> createToken(Token t);

    /**
     * retrieves the token if it is present and valid (not timed out)
     *
     * @return
     */
    IPromise<Token> takeToken(String tokenId, boolean delete);

    IPromise<String> getUserFromSessionId(String sid);
    void putUserAtSessionId(String sessionId, String userKey);

    void delUser(String userkey);
    IPromise<PersistedRecord> getUser(String userId);
    void putUser(PersistedRecord userRecord);
    IPromise atomicUpdate(String key, Function<PersistedRecord,Object> operation);
    IPromise<Boolean> putUserIfNotPresent(PersistedRecord userRecord);

    /**
     * stream all user records to the given callback and close it calling cb.finish()
     * @param cb
     */
    void forEachUser(Callback<PersistedRecord> cb);
}

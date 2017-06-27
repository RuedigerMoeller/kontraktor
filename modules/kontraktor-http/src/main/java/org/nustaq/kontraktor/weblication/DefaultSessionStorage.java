package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;

/**
 * Created by ruedi on 26.06.17.
 */
public class DefaultSessionStorage implements ISessionStorage {

    FSTAsciiStringOffheapMap sessionId2UserKey;
    FSTUTFStringOffheapMap userData;

    @Override
    public IPromise<String> getUserFromSessionId(String sid) {
        return new Promise(sessionId2UserKey.get(sid));
    }

    @Override
    public void storeUserRecord(String userId, Object userRecord) {
        userData.put(userId,userRecord);
    }

    @Override
    public IPromise getUserRecord(String userId) {
        return new Promise(userData.get(userId));
    }
}

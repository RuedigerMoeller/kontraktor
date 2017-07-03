package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;

import java.util.function.Function;

/**
 * Created by ruedi on 26.06.17.
 *
 * Kep async as in could be implemented by a database or a (scalable,remote) noSQL e.g. RealLive storage
 */
public class DefaultSessionStorage implements ISessionStorage {

    FSTAsciiStringOffheapMap sessionId2UserKey;
    FSTUTFStringOffheapMap userData;

    @Override
    public IPromise<String> getUserFromSessionId(String sid) {
        return new Promise(sessionId2UserKey.get(sid));
    }

    @Override
    public IPromise atomic(String userId, Function<Object, AtomicResult> recordConsumer) {
        AtomicResult res = recordConsumer.apply(userData.get(userId));
        if ( res.getAction() == Action.PUT ) {
            userData.put(userId,res.getRecord());
        } else if ( res.getAction() == Action.DELETE ) {
            userData.remove(userId);
        }
        return new Promise(res.getReturnValue());
    }

    @Override
    public void storeUserRecord(String userId, Object userRecord) {
        userData.put(userId,userRecord);
    }

    @Override
    public IPromise storeIfNotPresent(String userId, Object userRecord) {
        return null;
    }

    @Override
    public IPromise getUserRecord(String userId) {
        return new Promise(userData.get(userId));
    }

}

package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by ruedi on 26.06.17.
 *
 * Kep async as in could be implemented by a database or a (scalable,remote) noSQL e.g. RealLive storage
 */
public class DefaultSessionStorage extends Actor<DefaultSessionStorage> implements ISessionStorage {

    public static class Config implements Serializable {

        long sizeSessionIdsGB = 1024*1024*1024L;
        long sizeUserDataGB = 10*1024*1024*1024L;

        public long getSizeSessionIdsGB() {
            return sizeSessionIdsGB;
        }

        public Config sizeSessionIdGB(long sizeGB) {
            this.sizeSessionIdsGB = sizeGB;
            return this;
        }

        public long getSizeUserDataGB() {
            return sizeUserDataGB;
        }


        public Config sizeSessionIdsGB(long sizeSessionIdsGB) {
            this.sizeSessionIdsGB = sizeSessionIdsGB;
            return this;
        }

        public Config sizeUserDataGB(long sizeUserDataGB) {
            this.sizeUserDataGB = sizeUserDataGB;
            return this;
        }
    }

    FSTAsciiStringOffheapMap sessionId2UserKey;
    FSTUTFStringOffheapMap userData;

    public IPromise init(Config cfg) {
        try {
            sessionId2UserKey = new FSTAsciiStringOffheapMap("./data/sessionid2userkey.oos", 64, cfg.getSizeSessionIdsGB(), 1_000_000);
            userData = new FSTUTFStringOffheapMap("./data/sessionid2userkey.oos", 64, cfg.getSizeUserDataGB(), 1_000_000);
        } catch (Exception e) {
            Log.Warn(this,e);
            return new Promise(null,e);
        }
        return new Promise(true);
    }

    @Override
    public IPromise<String> getUserKeyFromSessionId(String sid) {
        return new Promise(sessionId2UserKey.get(sid));
    }

    @Override
    public IPromise atomic(String userId, Function<PersistedRecord, AtomicResult> recordConsumer) {
        AtomicResult res = recordConsumer.apply((PersistedRecord) userData.get(userId));
        if ( res.getAction() == Action.PUT ) {
            userData.put(userId,res.getRecord());
        } else if ( res.getAction() == Action.DELETE ) {
            userData.remove(userId);
        }
        return new Promise(res.getReturnValue());
    }

    @Override
    public void storeRecord(String userId, PersistedRecord userRecord) {
        userData.put(userId,userRecord);
    }

    @Override
    public IPromise<Boolean> storeIfNotPresent(String userId, PersistedRecord userRecord) {
        Object res = userData.get(userId);
        if ( res == null ) {
            userData.put(userId,userRecord);
            return new Promise(true);
        }
        return new Promise(false);
    }

    @Override
    public IPromise getUserRecord(String userId) {
        return new Promise(userData.get(userId));
    }

}

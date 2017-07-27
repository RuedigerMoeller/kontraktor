package org.nustaq.kontraktor.weblication.model;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.UUID;
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
            new File("./run/data").mkdirs();
            sessionId2UserKey = new FSTAsciiStringOffheapMap("./run/data/sessionid2userkey.oos", 64, cfg.getSizeSessionIdsGB(), 1_000_000);
            userData = new FSTUTFStringOffheapMap("./run/data/userdata.oos", 64, cfg.getSizeUserDataGB(), 1_000_000);
        } catch (Exception e) {
            Log.Warn(this,e);
            return new Promise(null,e);
        }
        return new Promise(true);
    }

    @Override
    public IPromise<String> createToken(Token t) {
        String key = UUID.randomUUID().toString();
        sessionId2UserKey.put(key,
            new PersistedRecord(key)
                .put("lifeTime", System.currentTimeMillis()+t.getLifeTime())
                .put("userId",t.getUserId())
                .put("data",t.getData())
        );
        return resolve(key);
    }

    @Override
    public IPromise<Token> takeToken(String tokenId, boolean delete) {
        Object o = sessionId2UserKey.get(tokenId);
        if ( o instanceof PersistedRecord ) {
            PersistedRecord r = (PersistedRecord) o;
            // still valid ?
            if ( r.getLong("lifeTime") > System.currentTimeMillis() ) {
                if ( delete ) {
                    sessionId2UserKey.remove(tokenId);
                }
                return resolve(new Token(r.getString("userId"),r.getString("data"),r.getLong("lifeTime")));
            } else {
                sessionId2UserKey.remove(tokenId); // delete outdated token
            }
        }
        return resolve(null);
    }

    @Override
    public IPromise<String> getUserFromSessionId(String sid) {
        Object o = sessionId2UserKey.get(sid);
        if ( o instanceof Object[] ) { // backward
            o = ((Object[]) o)[0];
        }
        return new Promise(o);
    }

    @Override
    public void putUserAtSessionId(String sessionId, String userKey) {
        sessionId2UserKey.put(sessionId, new Object[]{userKey,System.currentTimeMillis()});
    }

    @Override
    public void putUser(PersistedRecord userRecord) {
        userData.put(userRecord.getKey(),userRecord);
    }

    @Override
    public IPromise atomicUpdate(String key, Function<PersistedRecord, Object> operation) {
        try {
            Object ur = userData.get(key);
            Object result = operation.apply((PersistedRecord) ur);
            if (ur != null) {
                userData.put(key, ur);
            }
            return resolve(result);
        } catch (Exception ex) {
            return reject(ex);
        }
    }

    @Override
    public void delUser(String userkey) {
        userData.remove(userkey);
    }

    @Override
    public IPromise<Boolean> putUserIfNotPresent(PersistedRecord userRecord) {
        Object res = userData.get(userRecord.getKey());
        if ( res == null ) {
            userData.put(userRecord.getKey(),userRecord);
            return new Promise(true);
        }
        return new Promise(false);
    }

    @Override
    public IPromise getUser(String userId) {
        return new Promise(userData.get(userId));
    }

    @Override
    public void forEachUser(Callback<PersistedRecord> cb) {
        for (Iterator it = userData.values(); it.hasNext(); ) {
            PersistedRecord rec = (PersistedRecord) it.next();
            cb.stream(rec);
        }
        cb.finish();
    }

}

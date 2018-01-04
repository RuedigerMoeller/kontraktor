package org.nustaq.kontraktor.weblication.model;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.client.EmbeddedRealLive;
import org.nustaq.reallive.records.MapRecord;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ruedi on 26.06.17.
 *
 * Kep async as in could be implemented by a database or a (scalable,remote) noSQL e.g. RealLive storage
 */
public class DefaultSessionStorage extends Actor<DefaultSessionStorage> implements ISessionStorage {

    public static class Config implements Serializable {

        int sizeSessionIdsMB = 500;
        int sizeUserDataMB = 500;
        String dataDir = "./run/data/";

        public int getSizeSessionIdsMB() {
            return sizeSessionIdsMB;
        }

        public String getDataDir() {
            return dataDir;
        }

        public Config sizeSessionIdMB(int sizeGB) {
            this.sizeSessionIdsMB = sizeGB;
            return this;
        }

        public int getSizeUserDataMB() {
            return sizeUserDataMB;
        }


        public Config sizeSessionIdsMB(int sizeSessionIdsGB) {
            this.sizeSessionIdsMB = sizeSessionIdsGB;
            return this;
        }

        public Config sizeUserDataMB(int sizeUserDataGB) {
            this.sizeUserDataMB = sizeUserDataGB;
            return this;
        }

        public Config dataDir(String dataDir) {
            this.dataDir = dataDir;
            return this;
        }
    }

    RealLiveTable sessionId2UserKey;
    RealLiveTable userData;

    public IPromise init(Config cfg) {
        Promise p = new Promise();
        // FIXME: fireasync and await both
        try {
            String dataDir = cfg.getDataDir();
            new File(dataDir).mkdirs();
            IPromise _sessionId2UserKey = EmbeddedRealLive.get().createTable(
                new TableDescription()
                    .keyLen(64)
                    .sizeMB(cfg.getSizeSessionIdsMB())
                    .name("sessionid2userkey")
                    .storageType(TableDescription.StorageType.PERSIST),
                dataDir
            );
            IPromise _userData = EmbeddedRealLive.get().createTable(
                new TableDescription()
                    .keyLen(64)
                    .sizeMB(cfg.getSizeSessionIdsMB())
                    .name("userdata")
                    .storageType(TableDescription.StorageType.PERSIST),
                dataDir
            );
            Actors.all(_sessionId2UserKey,_userData).then( () -> {
               sessionId2UserKey = (RealLiveTable) _sessionId2UserKey.get();
               userData = (RealLiveTable) _userData.get();
               p.resolve();
            });
        } catch (Exception e) {
            Log.Warn(this,e);
            return new Promise(null,e);
        }
        return p;
    }

    @Override
    public IPromise<String> createToken(Token t) {
        String key = UUID.randomUUID().toString();
        sessionId2UserKey.setRecord(
            MapRecord.New(key)
                .put("lifeTime", System.currentTimeMillis()+t.getLifeTime())
                .put("userId",t.getUserId())
                .put("data",t.getData())
        );
        return resolve(key);
    }

    @Override
    public IPromise<Token> takeToken(String tokenId, boolean delete) {
        Promise p = new Promise();
        sessionId2UserKey.atomic(tokenId, r -> {
            if ( r != null) {
                // still valid ?
                if ( r.getLong("lifeTime") > System.currentTimeMillis() ) {
                    if ( delete ) {
                        sessionId2UserKey.remove(tokenId);
                    }
                    return new Token(r.getString("userId"),r.getString("data"),r.getLong("lifeTime"));
                } else {
                    sessionId2UserKey.remove(tokenId); // delete outdated token
                }
            }
            return null;
        }).then(p);
        return p;
    }

    @Override
    public IPromise<String> getUserFromSessionId(String sid) {
        Promise p = new Promise();
        sessionId2UserKey.get(sid).then( (rec,err) -> {
            if ( rec != null)
                p.resolve(rec.getString("user"));
            else
                p.complete(rec,err);
        });
        return p;
    }

    @Override
    public void putUserAtSessionId(String sessionId, String userKey) {
        sessionId2UserKey.merge(sessionId, "user",userKey);
    }

    @Override
    public void putUser(Record userRecord) {
        userData.setRecord(userRecord);
    }

    @Override
    public void delUser(String userkey) {
        userData.remove(userkey);
    }

    @Override
    public IPromise<Boolean> putUserIfNotPresent(Record userRecord) {
        Promise p = promise();
        userData.addRecord(userRecord).then( p );
        return p;
    }

    @Override
    public IPromise<Record> getUser(String userId) {
        return userData.get(userId);
    }

    @Override
    public void forEachUser(Callback<Record> cb) {
        userData.forEach( rec -> true, cb);
    }

}

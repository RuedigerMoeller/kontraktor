package org.nustaq.reactsample;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebSessionActor;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 01.07.17.
 */
public class ReactAppSession extends BasicWebSessionActor {

    Record userRecord;

    public void updateUserRecord(Record record) {
        userRecord = record;
    }

    @Override
    protected void persistSessionData(String sessionId, ISessionStorage storage) {
        Log.Info(this,"persistSessionData " + sessionId);
        // ideally write through persistence should be favored,
        // else newer data could be overwritten if a user has several client instances opened.
        // In addition, a server crash might loose unsaved data.
        // for some apps, temporary unsaved session state could speed up things e.g. gaming / financial trading
    }

    @Override
    protected IPromise loadSessionData(String sessionId, ISessionStorage storage) {
        Promise res = new Promise();
        Log.Info(this,"loadSessionData " + sessionId);
        // let's cache the record of current user (take care => stale state in case of multiple clients from same user)
        storage.getUser(getUserKey()).then( (user, err) -> {
            if ( user != null) {
                userRecord = user;
                res.resolve(userRecord);
            } else {
                res.reject(err);
            }
        });
        return res;
    }

    @Remoted
    public void delUser(String key) {
        getSessionStorage().delUser(key);
    }

    @Remoted
    public void queryUsers(Callback<Record> cb) {
        getSessionStorage().forEachUser( cb );
    }

    @Remoted
    public IPromise<String> createTokenLink() {
        Promise res = new Promise();
        getSessionStorage().createToken(
            new ISessionStorage.Token(userKey,"hello",60_000l)
        ).then( (s,err) -> {
            if ( s != null )
                res.resolve("direct/"+s);
            else
                res.reject(err);
        });
        return res;
    }
}

package org.nustaq.reactsample;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebSessionActor;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.kontraktor.weblication.PersistedRecord;

/**
 * Created by ruedi on 01.07.17.
 */
public class ReactAppSession extends BasicWebSessionActor {

    PersistedRecord userRecord;

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
        storage.getUserRecord(_getUserKey()).then( (user,err) -> {
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
        getSessionStorage().delRecord(key);
    }

    @Remoted
    public void queryUsers(Callback<PersistedRecord> cb) {
        getSessionStorage().forEachUser( cb );
    }

}

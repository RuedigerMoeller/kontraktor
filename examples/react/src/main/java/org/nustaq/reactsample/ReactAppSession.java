package org.nustaq.reactsample;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.weblication.BasicWebSessionActor;
import org.nustaq.kontraktor.weblication.DefaultSessionStorage;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.kontraktor.weblication.PersistedRecord;

/**
 * Created by ruedi on 01.07.17.
 */
public class ReactAppSession extends BasicWebSessionActor {

    @Override
    protected void persistSessionData(String sessionId, ISessionStorage storage) {
        Log.Info(this,"persistSessionData " + sessionId);
    }

    @Override
    protected void loadSessionData(String sessionId, ISessionStorage storage) {
        Log.Info(this,"loadSessionData " + sessionId);
    }

    @Remoted
    public void queryUsers(Callback<PersistedRecord> cb) {
        getSessionStorage().forEach( cb );
    }

}

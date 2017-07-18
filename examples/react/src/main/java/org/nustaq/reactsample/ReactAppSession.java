package org.nustaq.reactsample;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.weblication.BasicWebSessionActor;
import org.nustaq.kontraktor.weblication.DefaultSessionStorage;
import org.nustaq.kontraktor.weblication.ISessionStorage;
import org.nustaq.kontraktor.weblication.PersistedRecord;

/**
 * Created by ruedi on 01.07.17.
 */
public class ReactAppSession extends BasicWebSessionActor {

    @Override
    protected void persistSession(String sessionId, ISessionStorage storage) {
//        storage.
    }

    public void queryUsers(Callback<PersistedRecord> cb) {
        getSessionStorage().forEach( cb );
    }

}

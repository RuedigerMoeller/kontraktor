package org.nustaq.http.example;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.weblication.BasicWebSessionActor;
import org.nustaq.kontraktor.weblication.ISessionStorage;

import java.util.Date;

/**
 * Created by ruedi on 20.06.17.
 */
public class ServletSession extends BasicWebSessionActor<ServletSession> {

    public IPromise<String> whatsYourName() {
        System.out.println("whatsYourName " + userKey);
        return new Promise<>("Rüdiger "+ userKey);
    }

    public void push(Callback<String> cb) {
        if ( ! isStopped() ) {
            cb.stream(""+new Date());
            delayed(1000, () -> push(cb) );
        }
    }

    @Override
    protected void persistSessionData(String sessionId, ISessionStorage storage) {

    }

    @Override
    protected IPromise loadSessionData(String sessionId, ISessionStorage storage) {
        return new Promise("OK");
    }
}

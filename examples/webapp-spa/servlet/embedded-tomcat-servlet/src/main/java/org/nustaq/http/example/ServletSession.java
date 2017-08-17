package org.nustaq.http.example;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.util.Log;

import java.util.Date;

/**
 * Created by ruedi on 20.06.17.
 */
public class ServletSession extends Actor<ServletSession> implements RemotedActor{

    String user;

    public void init(String user) {
        this.user = user;
    }

    public IPromise<String> whatsYourName() {
        System.out.println("whatsYourName " + user);
        return new Promise<>("Hello "+ user);
    }

    public void push(Callback<String> cb) {
        if ( ! isStopped() ) {
            cb.pipe(""+new Date());
            delayed(1000, () -> push(cb) );
        }
    }

    @Override
    public void hasBeenUnpublished() {
        Log.Info(this,"client closed "+user);
    }
}

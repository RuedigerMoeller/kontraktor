package org.nustaq.reactsample;

import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.annotations.Secured;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;
import org.nustaq.kontraktor.weblication.PersistedRecord;


/**
 * Created by ruedi on 30.05.17.
 */
@Secured // enforce explicit tagging of remoteable methods (@Remote) effects ALSO Session actors
public class ReactApp extends BasicWebAppActor<ReactApp,BasicWebAppConfig> {

    @Override
    protected Class getSessionClazz() {
        return ReactAppSession.class;
    }

    @Remoted
    public IPromise register(String nick, String pwd, String text) {
        Promise p = new Promise();
        sessionStorage.storeIfNotPresent(new PersistedRecord(nick).put("pwd",pwd).put("text",text).put("verified",true)).then( (r,e) -> {
            if ( ! r ) {
                p.reject("user "+nick+" already exists");
            } else {
                p.resolve(true);
            }
        });
        return p;
    }

    /**
     * return new Promise(null) in order to bypass session reanimation default implementation
     */
//    @Override
//    public IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
//        Log.Info(this,"reanimate called ..");
//        return new Promise<>(null);
//    }

}

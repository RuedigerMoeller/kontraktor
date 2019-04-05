package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.Subscriber;
import org.nustaq.serialization.coders.Unknown;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * must be applied to SessionActor
 *
 * change user profile / pw change
 */
public interface SessionHandlingSessionMixin {

    @CallerSideMethod
    @Local
    DataClient getDClient();

    @CallerSideMethod @Local
    UserRecord getUser();

    @CallerSideMethod @Local
    List<Subscriber> getSubscriptions(); // collects subscriptions to be unsubscribed at session termination

    default IPromise saveProfile(Unknown data) {
        Promise p = new Promise();
        String pwd = data.getString("pwd");
        String verifyPwd = data.getString("verifyPwd");
        String oldPwd = data.getString("oldPwd");

        data.getFields().forEach( (k,v) -> {
            if ( !"pwd".equals(k) && !"verifyPwd".equals(k) && !"oldPwd".equals(k) && ! "name".equals(k) )
                getUser().put(k,v);
        });
        if ( pwd != null && pwd.length() > 0 ) {
            if ( ! getUser().getPwd().equals(oldPwd) ) {
                p.reject("altes Passwort ist falsch, das Passwort wurde nicht geändert");
            } else if ( verifyPwd == null || ! verifyPwd.equals(pwd) ){
                p.reject("neues Passwort wurde falsch wiederholt");
            } else if ( pwd.length() < 6 || pwd.length() > 20 ) {
                p.reject("neues Passwort muss zwischen 6 und 20 Zeichen lang sein");
            } else {
                getUser().pwd(pwd); // assume this changes original copy !
                getDClient().tbl(RegistrationMixin.UserTableName).setRecord(getUser().getRecord());
                p.resolve("Daten und Passwort geändert");
            }
        } else {
            getDClient().tbl(RegistrationMixin.UserTableName).setRecord(getUser().getRecord());
            p.resolve("Daten geändert");
        }
        return p;
    }

    default void subscribeUserRecord(UserRecord user, Callback uiSubscription, AtomicBoolean initialDataDone) {
        getSubscriptions().add(getDClient().tbl("user").subscribeOn( "_key == '"+user.getEmail()+"'", change -> {
            // need to wait for initial query before promoting changes to user object
            if ( change.isDoneMsg() )
                initialDataDone.set(true);
            else if ( initialDataDone.get() ){
                uiSubscription.pipe(new SessionEvent("user", change));
            }
        }));
    }
}

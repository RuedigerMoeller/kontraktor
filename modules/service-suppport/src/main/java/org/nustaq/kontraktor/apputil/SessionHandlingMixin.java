package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.SessionResurrector;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.Record;

/**
 * must be applied to ServerActor.
 *
 * @param <SELF>
 */
public interface SessionHandlingMixin<SELF extends Actor<SELF>> extends SessionResurrector {

    public static String TableName = "session2user";

    @CallerSideMethod
    @Local
    DataClient getDClient();

    /**
     * restore an old session (client has been inactive and "woke up")
     *
     * @param sessionId
     * @param remoteRefId
     * @return
     */
    @Override @Local
    default IPromise<Actor> reanimate(String sessionId, long remoteRefId) {
        // dummy in memory
        Record rec = getDClient().tbl(TableName).get(sessionId).await();
        if ( rec != null ) {
            // create a new session with stored data, client is notified
            // in case it needs to refresh client side data
            Log.Info(this,"reanimated session "+sessionId+" with data "+rec);
            return (IPromise)login(rec.getSafeString("userName"),rec.getSafeString("pwd"), null ); // fixme: manual event subscription required
        }
        return new Promise<>(null); // cannot reanimate => client shows "session expired"
    }

    /**
     * returns logindata including session or session
     *
     * @param username
     * @param pwd
     * @return
     */
    IPromise login(String username, String pwd, Callback events );

    /**
     * register a session for reanimation
     *
     * @param id
     * @param userName
     * @param pwd
     */
    @Local
    default void registerSessionData(String id, String userName, String pwd) {
        getDClient().tbl(TableName).update(id, "userName", userName, "pwd",pwd );
    }


}

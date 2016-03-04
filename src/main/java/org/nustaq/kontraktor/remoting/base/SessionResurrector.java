package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 04/03/16.
 */
public interface SessionResurrector {
    /**
     * currently only called in case of http remoting. An unknown remote ref was detected,
     * notify a new connection has been established.
     */
    void restoreRemoteRefConnection(String sessionId);

    /**
     * reanimate a remote ref of a resurrected facade connection
     *
     * @param sessionId
     * @param remoteRefId
     * @return
     */
    IPromise<Actor> reanimate(String sessionId, long remoteRefId); // only called in case of http remoting
}

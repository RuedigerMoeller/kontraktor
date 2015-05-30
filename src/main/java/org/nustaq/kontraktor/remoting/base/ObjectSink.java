package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.IPromise;

import java.util.List;

/**
 * Created by ruedi on 09/05/15.
 *
 * an object able to process decoded incoming messages
 */
public interface ObjectSink {

    /**
     *
     * @param sink - usually this or a wrapper of this
     * @param received - decoded object(s)
     * @param createdFutures - list of futures/callbacks contained in the decoded object remote calls (unused)
     * @param channelId - indicator for io channel (e.g. websocket text/binary)
     */
    void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures, int channelId);
    default void receiveObject(Object received, List<IPromise> createdFutures, int channelId) {
        receiveObject(this,received,createdFutures, channelId);
    }
    void sinkClosed();

}


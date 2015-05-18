package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.util.Log;

import java.util.List;

/**
 * Created by ruedi on 09/05/15.
 *
 * an object able to process decoded incoming messages
 */
public interface ObjectSink {

    void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures);
    default void receiveObject(Object received, List<IPromise> createdFutures) {
        receiveObject(this,received,createdFutures);
    }
    void sinkClosed();

}


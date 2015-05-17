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
    void sinkClosed();

    // sequencing is unimplemented by default

    default void receiveObject(Object received, List<IPromise> createdFutures) {
        receiveObject(this, received, createdFutures);
    }

    default int getLastSinkSequence() {
        return -1;
    }

    default void setLastSinkSequence(int ls) {
    }

    default Object takeStoredMessage(int seq) {
        return null;
    }

    default void storeGappedMessage(int inSequence, Object response) {
        Log.Warn(this, "gap handling not supported");
    }
}


package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 09/05/15.
 *
 * an object able to process decoded incoming messages
 */
public interface ObjectSink {

    public void receiveObject(Object received);
    public void sinkClosed();

    // sequencing is unimplemented by default

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


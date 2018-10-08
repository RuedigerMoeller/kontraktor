package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

public interface TrackingClient {

    /**
     * to be called whenever a request from an external system is incoming
     */
    default void externalBoundaryIncoming(Actor receiver, String method, Object arg[] ) {

    }
}

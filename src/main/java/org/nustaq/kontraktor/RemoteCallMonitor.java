package org.nustaq.kontraktor;

import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

/**
 * receives all remote method calls as well as promises / callback responses received from remote.
 * It's static delegate, inject an implementation at Actor.RemoteCallMonitoring
 */
public interface RemoteCallMonitor {

    <SELF extends Actor> void remoteCallObserved(Actor selfActor, ObjectSocket objSocket, RemoteCallEntry rce, Object authContext);
}

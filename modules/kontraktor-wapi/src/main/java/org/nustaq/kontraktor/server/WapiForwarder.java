package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.Forwarder;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

/**
 * Created by ruedi on 13.03.17.
 */
public class WapiForwarder extends Actor implements Forwarder {

    Actor remoteRef;

    public void remoteRef(Actor remoteRef) {
        this.remoteRef = remoteRef;
    }

    @Override @CallerSideMethod
    public boolean forward(RemoteCallEntry read) {
        return false;
    }
}

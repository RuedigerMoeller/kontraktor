package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

import java.util.List;

/**
 * this is a dummy implementation as runtime generation of classes does not allow
 * proxies on abstract classes
 *
 */
public class Krouter extends AbstractKrouter<Krouter> {
    @Override
    public IPromise router$RegisterService(Actor remoteRef) {
        return null;
    }

    @Override
    public void router$handleServiceDisconnect(Actor disconnected) {
    }

    @Override
    protected List<Actor> getServices() {
        return null;
    }

    @Override
    protected boolean dispatchRemoteCall(RemoteCallEntry rce, ConnectionRegistry clientRemoteRegistry) {
        return false;
    }
}

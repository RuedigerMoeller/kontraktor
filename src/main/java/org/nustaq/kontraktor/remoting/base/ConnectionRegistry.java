package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;

/**
 * public interface to shield RemoteRegistry from user code
 */
public class ConnectionRegistry {

    private final RemoteRegistry reg;

    public ConnectionRegistry(RemoteRegistry reg) {
        this.reg = reg;
    }

    public boolean isTerminated() {
        return reg.isTerminated();
    }

    public Actor getFacade() {
        return reg.getFacadeActor();
    }

    public long getLastClientPing() {
        return reg.getLastPing();
    }

    public void ping() {
        reg.ping();
    }

    public String getConnectionId() {
        ObjectSocket objectSocket = reg.getWriteObjectSocket().get();
        if (objectSocket!=null)
            return objectSocket.getConnectionIdentifier();
        return null;
    }
}

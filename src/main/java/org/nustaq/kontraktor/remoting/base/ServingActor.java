package org.nustaq.kontraktor.remoting.base;

public interface ServingActor {

    void clientConnected(ConnectionRegistry connectionRegistry, String connectionIdentifier);
    void clientDisconnected(ConnectionRegistry connectionRegistry, String connectionIdentifier);

}

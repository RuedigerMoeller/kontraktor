package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;

/**
 * Created by ruedi on 19/05/15.
 */
public class HttpConnectable<T extends Actor> implements ConnectableActor<T> {

    @Override
    public IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return null;
    }

    @Override
    public IPromise close() {
        return null;
    }
}
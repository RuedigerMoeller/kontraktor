package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;

/**
 * Created by ruedi on 19/05/15.
 *
 * A connectable simply connecting to a local actor. A close connection event will never happen
 * and close is NOP.
 *
 */
public class LocalConnectable<T extends Actor> implements ConnectableActor<T>  {

    T actor;

    public LocalConnectable(T actor) {
        this.actor = actor;
    }

    @Override
    public IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return new Promise<>(actor);
    }

    @Override
    public IPromise close() {
        return new Promise<>(null);
    }

}

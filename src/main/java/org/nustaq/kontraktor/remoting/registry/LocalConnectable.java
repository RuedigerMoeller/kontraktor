package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;

/**
 * Created by ruedi on 19/05/15.
 *
 * A connectable simply connecting to a local actor. A close connection event will never happen (FIXME: send on stop instead)
 *
 */
public class LocalConnectable implements ConnectableActor  {

    Actor actor;

    public LocalConnectable(Actor actor) {
        this.actor = actor;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return new Promise<>((T) actor);
    }

}

/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;

/**
 * Created by ruedi on 19/05/15.
 *
 * A connectable simply connecting to a local actor. A close connection event will never happen (FIXME: send on stop instead)
 *
 */
public class LocalConnectable implements ConnectableActor {

    Actor actor;

    public LocalConnectable(Actor actor) {
        this.actor = actor;
    }

    /**
     * disconnect callback will never be called (local actor connection)
     * @param disconnectCallback
     * @param <T>
     * @return
     */
    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return new Promise<>((T) actor);
    }

    @Override
    public ConnectableActor actorClass(Class actorClz) {
        if ( ! actorClz.isAssignableFrom(actor.getClass())) {
            throw new RuntimeException("actor class mismatch");
        }
        return this;
    }

    public Actor getActor() {
        return actor;
    }
}

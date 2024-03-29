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
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Created by ruedi on 18/05/15.
 *
 * Describes a connectable remote or local actor. (implementations hold technical details such as host port etc.)
 *
 * To connect to a remote actor usually a connector is configured, then calling connect returns the reference to
 * the remote actor.
 *
 * In peer to peer actor remoting model, this can be passed around (over network) in order to
 * tellMsg other actors/services on how to connect a certain actor directly (e.g. a Service Registry would
 * pass ConnectableActors to clients).
 *
 */
public interface ConnectableActor extends Serializable {

    /**
     *
     * @param disconnectCallback - a callback called on disconnect, passing the ActorClientConnector instance
     * @param actorDisconnecCB - a consumer called on disconnect passing the remoteactor ref. Rarely needed. added to avoid braking things
     * @param <T>
     * @return
     */
    <T extends Actor> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback, Consumer<Actor> actorDisconnecCB);

    default <T extends Actor> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return this.connect(disconnectCallback,null);
    }

    default <T extends Actor> IPromise<T> connect() {
        return this.connect(null,null);
    }

    ConnectableActor actorClass( Class actorClz );
    Class<? extends Actor> getActorClass();
    ConnectableActor inboundQueueSize(final int inboundQueueSize);

    String getKey();
}

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
import org.nustaq.kontraktor.IPromise;

import java.util.function.Consumer;

/**
 * Configuration object to publish an actor to network. Serverside aequivalent to ConnectableActor
 */
public interface ActorPublisher {

    default IPromise<ActorServer>  publish() {
        return publish(null);
    }
    IPromise<ActorServer>  publish(Consumer<Actor> disconnectCallback);
    ActorPublisher facade( Actor facade );
    void setTrafficMonitor(TrafficMonitor trafficMonitor);

}

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
import java.util.function.Function;

/**
 * Created by ruedi on 09/05/15.
 * Interface unifying publishing an actor via network (the thingy translating remote calls to local calls).
 * Mostly of internal interest.
 */
public interface ActorServerConnector {

    void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception;
    IPromise closeServer();

}

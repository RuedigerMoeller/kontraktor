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

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.service.ServiceConstraints;

import java.util.List;

/**
 * Created by ruedi on 09/05/15.
 *
 * an object able to process decoded incoming messages
 */
public interface ObjectSink {

    /**
     * @param sink - usually this or a wrapper of this
     * @param received - decoded object(s)
     * @param createdFutures - list of futures/callbacks contained in the decoded object remote calls (unused)
     * @param securityContext
     */
    void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures, Object securityContext);
    default void receiveObject(Object received, List<IPromise> createdFutures, Object securityContext) {
        receiveObject(this,received,createdFutures, securityContext);
    }
    void sinkClosed();
    default ServiceConstraints getConstraints() {
        return null;
    }
}


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

package org.nustaq.kontraktor.impl;

/**
 * Created by ruedi on 30.10.14.
 *
 * if an actor is blocked because a receiver's queue is full
 */
public class ActorBlockedException extends RuntimeException {

    public static ActorBlockedException Instance = new ActorBlockedException();
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}

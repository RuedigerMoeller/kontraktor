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

package org.nustaq.kontraktor;


/**
 * Created by ruedi on 24.08.2014.
 */
public interface RemoteConnection {
    /**
     * closes the underlying connection (Warning: may side effect to other actors published on this connection)
     */
    void close();
    void setClassLoader( ClassLoader l );
    int getRemoteId( Actor act );

    /**
     * unpublishes this actor by removing mappings and stuff. Does not actively close the underlying connection
     * @param self
     *
     */
    void unpublishActor(Actor self);


    /**
     * closes underlying network connection also. Can be dangerous as other clients might share it
     */
    IPromise closeNetwork();
}

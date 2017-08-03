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
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Date: 04.01.14
 * Time: 14:22
 */

import java.io.Serializable;

/**
 * Typically used to receive/stream results from outside the actor.
 * The underlying mechanics scans method arguments and schedules calls on the call back into the calling actors thread.
 * Note that the callback invocation is added as a message to the end of the calling actor.
 * e.g. actor.method( arg, new Callbacl() { public void complete(T result, Object error ) { ..runs in caller thread.. } }
 */
public interface Callback<T> extends Serializable
{
    /**
     * use value as error to indicate more messages are to come (else remoting will close channel).
     */
    String CONT = "CNT";

    /**
     * set result or error. error might also contain flow indicators to signal end/continue of
     * stream when remoting. (Actor.FIN Actor.CONT)
     *
     * @param result
     * @param error
     */
    void complete(T result, Object error);

    /**
     * same as complete(null,null)
     */
    default void complete() {
        complete(null, null);
    }

    /**
     * same as complete(null,null) and resolve(null)
     */
    default void resolve() {
        complete(null, null);
    }

    /**
     * signal an error to sender. Will automatically "close" the callback if remoted.
     * same as complete( null, error );
     *
     * @param error
     */
    default void reject(Object error) {
        complete(null, error);
    }

    /**
     * pass a result object to the sender. This can be called only once (connection to sender will be closed afterwards).
     * same as complete( result, null );
     *
     * @param result
     */
    default void resolve(T result) {
        complete(result, null);
    }

    /**
     * invalid for Promises!. can be called more than once on Callback's in order to stream objects to the sender.
     * same as complete( result, CONT );
     *
     * @param result
     */
    default Callback pipe(T result) {
        complete(result, CONT);
        return this;
    }

    /**
     * signal end of streamed objects (required for remoteref housekeeping if actors run remotely)
     * same as complete( null, null );
     *
     */
    default void finish() {
        complete(null, null);
    }

    /**
     * relevant for remoted callback's
     *
     * @return true if the client owning this remote callback proxy has disconnected
     */
    default boolean isTerminated() {
        return false;
    }

}

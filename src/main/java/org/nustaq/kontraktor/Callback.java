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
 * Typically used to receive results from outside the actor.
 * The underlying mechanics scans method arguments and schedules calls on the call back into the calling actors thread.
 * Note that the callback invocation is added as a message to the end of the calling actor.
 * e.g. actor.method( arg, new Callbacl() { public void receive(T result, Object error ) { ..runs in caller thread.. } }
 */
public interface Callback<T> extends Serializable  // do not use interface, slows down instanceof significantly
{
    /**
     * use value to signal no more messages. THE RECEIVER CALLBACK WILL NOT SEE THIS MESSAGE.
     */
    public final String FINSILENT = "EOT";
    /**
     * use value as error to indicate more messages are to come (else remoting will close channel).
     */
    public final String CONT = "CNT";
    /**
     * use this value to signal no more messages. The receiver callback will receive the message.
     * Note that any value except CONT will also close the callback channel. So this is informal.
     */
    public final String FIN = "FIN";
    public void receive(T result, Object error);

}

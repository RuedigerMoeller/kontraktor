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

package org.nustaq.kontraktor.remoting.base.messagestore;

/**
 * Created by ruedi on 13/04/15.
 *
 * interface defining a message store capable of storing/retrieving sequenced messages.
 * used e.g. as ringbuffer to support retransmission of lost messages (e.g. reliable long poll)
 *
 */
public interface MessageStore {

    Object getMessage( CharSequence queueId, long sequence );
    void   putMessage( CharSequence queueId, long sequence, Object message );
    void   confirmMessage( CharSequence queueId, long sequence );

    void killQueue( CharSequence queueId);

}

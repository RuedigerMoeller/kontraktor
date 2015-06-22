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

package org.nustaq.kontraktor.asyncio;

import java.nio.ByteBuffer;

/**
 * Created by moelrue on 5/4/15.
 */
public class AsyncFileIOEvent {

    int read;
    long nextPosition;
    ByteBuffer buffer;

    public AsyncFileIOEvent(long position, int read, ByteBuffer buffer) {
        this.nextPosition = position;
        this.buffer = buffer;
        this.read = read;
    }

    public long getNextPosition() {
        return nextPosition;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public byte[] copyBytes() {
        byte b[] = new byte[buffer.limit() - buffer.position()];
        buffer.get(b);
        return b;
    }

    public int getRead() {
        return read;
    }

    @Override
    public String toString() {
        return "AsyncFileIOEvent{" +
                "read=" + read +
                ", nextPosition=" + nextPosition +
                ", buffer=" + buffer +
                '}';
    }


    public void reset() {
        buffer.position(0);
        buffer.limit(buffer.capacity());
    }
}

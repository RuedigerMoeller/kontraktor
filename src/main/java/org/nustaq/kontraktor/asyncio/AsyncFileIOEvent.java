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

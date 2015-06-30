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

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.serialization.util.*;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Baseclass for handling async io. Its strongly recommended to use QueuingAsyncSocketConnection as this
 * eases things.
 */
public abstract class AsyncSocketConnection {

    protected ByteBuffer readBuf = ByteBuffer.allocateDirect(4096);

    protected SelectionKey key;
    protected SocketChannel chan;

    protected Promise writePromise;
    protected ByteBuffer writingBuffer;
    boolean isClosed;

    public AsyncSocketConnection(SelectionKey key, SocketChannel chan) {
        this.key = key;
        this.chan = chan;
    }

    public void closed(Exception ioe) {
        System.out.println("connection closed " + ioe);
        isClosed = true;
    }

    public void close() throws IOException {
        chan.close();
    }

    /**
     * @return wether more reads are to expect
     * @throws IOException
     */
    boolean readData() throws IOException {
        checkThread();
        readBuf.position(0); readBuf.limit(readBuf.capacity());
        int read = chan.read(readBuf);
        if ( read == -1 )
            throw new EOFException("connection closed");
        readBuf.flip();
        if ( readBuf.limit() > 0 )
            dataReceived(readBuf);
        return read == readBuf.capacity();
    }

    protected void checkThread() {
        if ( debug == null )
            debug = Thread.currentThread();
        else if ( debug != Thread.currentThread() ) {
            throw new RuntimeException("unexpected multithreading");
        }
    }

    /**
     * writes given buffer content. In case of partial write, another write is enqueued internally.
     * once the write is completed, the returned promise is fulfilled.
     * the next write has to wait until the future has completed, else write order might get mixed up.
     *
     * Better use write* methods of QueuingAsyncSocketConnection as these will write to a binary queue
     * which is read+sent behind the scenes in parallel.
     *
     * @param buf
     * @return
     */
    Thread debug;
    protected IPromise directWrite(ByteBuffer buf) {
        checkThread();
        if ( writePromise != null )
            throw new RuntimeException("concurrent write con:"+chan.isConnected()+" open:"+chan.isOpen());
        writePromise = new Promise();
        writingBuffer = buf;
        Promise res = writePromise;
        try {
            int written = 0;
            written = chan.write(buf);
            if (written<0) {
                // TODO:closed
                writeFinished("disconnected");
            }
            if ( buf.remaining() > 0 ) {
//                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                writeFinished(null);
            }
        } catch (Exception e) {
            res.reject(e);
            FSTUtil.rethrow(e);
        }
        return res;
    }

    ByteBuffer getWritingBuffer() {
        return writingBuffer;
    }

    public boolean canWrite() {
        return writePromise == null;
    }


    void writeFinished(Object error) {
        checkThread();
        writingBuffer = null;
        Promise wp = this.writePromise;
        writePromise = null;
        if ( ! wp.isSettled() ) {
            if ( error != null )
                wp.reject(error);
            else
                wp.complete();
        }
    }

    public abstract void dataReceived(ByteBuffer buf);

    public boolean isClosed() {
        return !chan.isOpen() && ! isClosed;
    }
}

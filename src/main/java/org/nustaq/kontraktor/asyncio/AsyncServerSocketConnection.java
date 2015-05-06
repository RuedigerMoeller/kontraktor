package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Baseclass for handling async io. Its strongly recommended to use QueuingAsyncSocketConnection as this
 * eases things quite a lot.
 */
public abstract class AsyncServerSocketConnection {

    protected ByteBuffer readBuf = ByteBuffer.allocate(4096);
    protected SelectionKey key;
    protected SocketChannel chan;

    public AsyncServerSocketConnection(SelectionKey key, SocketChannel chan) {
        this.key = key;
        this.chan = chan;
    }

    public void closed(Exception ioe) {
        System.out.println("connection closed " + ioe);
    }

    public void readData() throws IOException {
        readBuf.position(0); readBuf.limit(readBuf.capacity());
        int read = chan.read(readBuf);
        if ( read == -1 )
            throw new EOFException("connection closed");
        readBuf.flip();
        if ( readBuf.limit() > 0 )
            dataReceived(readBuf);
    }

    /**
     * writes given buffer content. In case of partial write, another write is enqueued.
     * once the write is completed, the returned promise is fulfilled
     *
     * @param buf
     * @return
     */
    public IPromise directWrite(ByteBuffer buf) {
        return directWrite(buf,null);
    }

    protected IPromise directWrite(ByteBuffer buf, Promise result) {
        if ( result == null )
            result = new Promise();
        try {
            int written = chan.write(buf);
            if ( buf.remaining() > 0 ) {
                Actor actor = Actor.sender.get();
                if ( actor == null )
                    throw new RuntimeException("can only by called from within actor Thread");
                final Promise finalResult = result;
                actor.execute( () -> {
                    directWrite(buf, finalResult);
                });
            } else {
                result.complete();
            }
        } catch (IOException e) {
            result.reject(e);
        }
        return result;
    }

    public abstract void dataReceived(ByteBuffer buf);

}

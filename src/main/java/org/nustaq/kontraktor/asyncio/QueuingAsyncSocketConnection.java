package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.niobuffers.ByteBufferBasicBytez;
import org.nustaq.offheap.bytez.onheap.HeapBytez;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by moelrue on 5/5/15.
 *
 * A server socket connection which buffers incoming/outgoing data in a binary queue so
 * an application can easily parse and process data async in chunks without having
 * to maintain complex state machines.
 *
 */
public abstract class QueuingAsyncSocketConnection extends AsyncSocketConnection {

    public static long MAX_Q_SIZE_BYTES = 10_000_000;

    protected BinaryQueue readQueue = new BinaryQueue();
    protected BinaryQueue writeQueue = new BinaryQueue();

    protected ByteBufferBasicBytez wrapper = new ByteBufferBasicBytez(null);

    public QueuingAsyncSocketConnection(SelectionKey key, SocketChannel chan) {
        super(key, chan);
    }

    ByteBufferBasicBytez tmp = new ByteBufferBasicBytez(null);
    HeapBytez tmpBA = new HeapBytez(new byte[0]);

    protected void checkQSize() {
        if ( writeQueue.available() > MAX_Q_SIZE_BYTES ) {
            LockSupport.parkNanos(1);
        }
    }

    public void write( ByteBuffer buf ) {
        checkQSize();
        tmp.setBuffer(buf);
        writeQueue.add(tmp);
    }

    public void write( byte b[] ) {
        checkQSize();
        write(b, 0, b.length);
    }

    public void write( byte b[], int off, int len ) {
        checkQSize();
        tmpBA.setBase(b, off, len);
        writeQueue.add(tmpBA);
    }

    public void write( int val ) {
        checkQSize();
        writeQueue.addInt(val);
    }

    // quite some fiddling required to deal with various byte abstractions

    IPromise writeFuture; // fires once next chunk from queue can be taken
    ByteBuffer qWriteTmp = ByteBuffer.allocateDirect(128000);
    public void tryFlush() {
        if ( writeFuture == null) {
            qWriteTmp.position(0);
            qWriteTmp.limit(qWriteTmp.capacity());
            tmp.setBuffer(qWriteTmp);
            long poll = writeQueue.poll(tmp, 0, tmp.length());
//            System.out.println("try write "+poll+" avail:"+writeQueue.available()+" cap:"+writeQueue.capacity());
            if (poll > 0) {
                qWriteTmp.limit((int) poll);
                writeFuture = directWrite(qWriteTmp);
                writeFuture.then( (res,err) -> {
                    writeFuture = null;
                    if ( err != null) {
                        Log.Lg.error(this, (Throwable) err,"write failure");
                    } else {
                        tryFlush();
                    }
                });
            }
        }
    }

    @Override
    public void dataReceived(ByteBuffer buf) {
        wrapper.setBuffer(buf);
        readQueue.add(wrapper, buf.position(), buf.limit());
        dataReceived(readQueue);
    }

    protected abstract void dataReceived(BinaryQueue queue);

    public void closed(Exception ioe) {
    }

}

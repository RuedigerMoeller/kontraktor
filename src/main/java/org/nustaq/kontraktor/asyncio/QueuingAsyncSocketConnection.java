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

/**
 * Created by moelrue on 5/5/15.
 *
 * A server socket connection which buffers incoming/outgoing data in a binary queue so
 * an application can easily parse and process data async in chunks without having
 * to maintain complex state machines.
 *
 */
public abstract class QueuingAsyncSocketConnection extends AsyncServerSocketConnection {

    protected BinaryQueue readQueue = new BinaryQueue();
    protected BinaryQueue writeQueue = new BinaryQueue();

    protected ByteBufferBasicBytez wrapper = new ByteBufferBasicBytez(null);

    public QueuingAsyncSocketConnection(SelectionKey key, SocketChannel chan) {
        super(key, chan);
    }

    ByteBufferBasicBytez tmp = new ByteBufferBasicBytez(null);
    HeapBytez tmpBA = new HeapBytez(new byte[0]);

    public void write( ByteBuffer buf ) {
        if ( Thread.currentThread() instanceof DispatcherThread == false )
            throw new RuntimeException("noes");
        tmp.setBuffer(buf);
        writeQueue.add(tmp);
        tryWrite();
    }

    public void write( byte b[] ) {
        if ( Thread.currentThread() instanceof DispatcherThread == false )
            throw new RuntimeException("noes");
        write(b, 0, b.length);
        tryWrite();
    }

    public void write( byte b[], int off, int len ) {
        if ( Thread.currentThread() instanceof DispatcherThread == false )
            throw new RuntimeException("noes");
        tmpBA.setBase(b, off, len);
        writeQueue.add(tmpBA);
        tryWrite();
    }

    public void write( int val ) {
        if ( Thread.currentThread() instanceof DispatcherThread == false )
            throw new RuntimeException("noes");
        writeQueue.addInt(val);
        tryWrite();
    }

    // quite some fiddling required to deal with various byte abstractions

    IPromise writeFuture;
    ThreadLocal<ByteBuffer> qWriteTmpTH = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(16000);
        }
    };
    public void tryWrite() {
        if ( Thread.currentThread() instanceof DispatcherThread == false )
            throw new RuntimeException("noes");
        if ( writeFuture == null ) {
            ByteBuffer qWriteTmp = qWriteTmpTH.get();
            qWriteTmp.position(0);
            qWriteTmp.limit(qWriteTmp.capacity());
            tmp.setBuffer(qWriteTmp);
            long poll = writeQueue.poll(tmp, 0, tmp.length());
//            System.out.println("try write "+poll+" avail:"+writeQueue.available()+" cap:"+writeQueue.capacity());
            if (poll > 0) {
                qWriteTmp.limit((int) poll);
                writeFuture = directWrite(qWriteTmp);
                writeFuture.then( (res,err) -> {
                    if ( Thread.currentThread() instanceof DispatcherThread == false )
                        throw new RuntimeException("noes");
                    writeFuture = null;
                    if ( err != null) {
                        Log.Lg.error(this, (Throwable) err,"write failure");
                    } else {
                        tryWrite();
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

package org.nustaq.kontraktor.asyncio;

import org.nustaq.offheap.BinaryQueue;
import org.nustaq.offheap.bytez.niobuffers.ByteBufferBasicBytez;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by moelrue on 5/5/15.
 */
public abstract class RingbufferingAsyncSSConnection extends AsyncServerSocketConnection {

    protected BinaryQueue queue = new BinaryQueue();
    protected ByteBufferBasicBytez wrapper = new ByteBufferBasicBytez(null);

    public RingbufferingAsyncSSConnection(SelectionKey key, SocketChannel chan) {
        super(key, chan);
    }

    @Override
    public void dataReceived(ByteBuffer buf) {
        wrapper.setBuffer(buf);
        queue.add(wrapper, buf.position(), buf.limit());
        dataReceived(queue);
    }

    protected abstract void dataReceived(BinaryQueue queue);

}

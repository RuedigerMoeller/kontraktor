package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Predicate;

/**
 * Created by ruedi on 05/05/15.
 */
public abstract class AsyncServerSocketConnection {

    protected ByteBuffer buf = ByteBuffer.allocate(1024);
    protected SelectionKey key;
    protected SocketChannel chan;

    public AsyncServerSocketConnection(SelectionKey key, SocketChannel chan) {
        this.key = key;
        this.chan = chan;
    }

    public void closed(Exception ioe) {
        System.out.println("connection closed");
    }

    public void readData() throws IOException {
        int read = chan.read(buf);
        if ( read == -1 )
            throw new EOFException("connection closed");
        if ( buf.position() == buf.limit() ) {
            ByteBuffer newOne = ByteBuffer.allocate(buf.capacity()*2);
            buf.flip();
            newOne.put(buf);
            buf = newOne;
            readData();
        }
        buf.flip();
        if ( buf.limit() > 0 )
            dataReceived(buf);
    }

    public abstract void dataReceived(ByteBuffer buf);

//    protected Predicate<ByteBuffer> condition;
//    protected Promise<ByteBuffer> promise;
//
//    protected void testCondition() {
//        if ( condition.test(buf) ) {
//            buf.flip();
//            promise.resolve(buf);
//        }
//    }
//
//    public IPromise<ByteBuffer> when( Predicate<ByteBuffer> predicate ) {
//        condition = predicate;
//        promise = new Promise<>();
//        testCondition();
//        return promise;
//    }
//
//    public IPromise<ByteBuffer> whenAvailable(int bytez) {
//        return when(buffer -> buffer.position() >= bytez );
//    }
//
//    // fixme: maybe byte oriented stream interface fits better
//    // consider ringbuffering for reading
//    public void parse() {
//        whenAvailable(4).then( buf0 -> {
//            int len = buf0.getInt();
//            whenAvailable(len).then(buf1 -> {
//                //decoder.readObject(buffer);
//            });
//        });
//    }

}

package org.nustaq.kontraktor.asyncio;

import org.nustaq.offheap.BinaryQueue;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by moelrue on 5/7/15.
 */
public class ObjectAsyncSocketConnection extends QueuingAsyncSocketConnection {

    FSTConfiguration conf;

    public ObjectAsyncSocketConnection(FSTConfiguration conf, SelectionKey key, SocketChannel chan) {
        super(key, chan);
        this.conf = conf;
    }


    @Override
    public void dataReceived(BinaryQueue q) {
        while ( q.available() > 4 ) {
            int len = q.readInt();
            if ( q.available() >= len ) {
                byte[] bytes = q.readByteArray(len);
                receivedObject(conf.asObject(bytes));
            } else {
                q.back(4);
                break;
            }
        }
    }

    public void receivedObject(Object o) {

    }

    public void writeObject(Object o) {
        byte[] bytes = conf.asByteArray(o);
        write(bytes);
    }
}

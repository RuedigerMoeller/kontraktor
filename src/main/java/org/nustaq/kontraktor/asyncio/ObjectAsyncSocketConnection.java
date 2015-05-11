package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

/**
 * Created by moelrue on 5/7/15.
 */
public abstract class ObjectAsyncSocketConnection extends QueuingAsyncSocketConnection implements ObjectSocket {

    FSTConfiguration conf;
    Throwable lastError;
    ArrayList objects = new ArrayList();

    public ObjectAsyncSocketConnection(SelectionKey key, SocketChannel chan) {
        super(key, chan);
    }

    public ObjectAsyncSocketConnection(FSTConfiguration conf, SelectionKey key, SocketChannel chan) {
        super(key, chan);
        setConf(conf);
    }

    public void setConf(FSTConfiguration conf) {this.conf = conf;}

    public FSTConfiguration getConf() {
        return conf;
    }

    @Override
    public void dataReceived(BinaryQueue q) {
        while ( q.available() > 4 ) {
            int len = q.readInt();
            if ( len <= 0 )
            {
                System.out.println("object len ?? "+len);
                return;
            }
            if ( q.available() >= len ) {
                byte[] bytes = q.readByteArray(len);
                receivedObject(conf.asObject(bytes));
            } else {
                q.back(4);
                break;
            }
        }
    }

    public abstract void receivedObject(Object o);

    public void writeObject(Object o) {
        objects.add(o);
        if (objects.size()>100) {
            try {
                flush();
            } catch (Exception e) {
                Actors.throwException(e);
            }
        }
    }

    @Override
    public void flush() throws IOException, Exception {
        if ( objects.size() == 0 ) {
            return;
        }
        objects.add(0); // sequence
        Object[] objArr = objects.toArray();
        objects.clear();

        byte[] bytes = conf.asByteArray(objArr);
        write(bytes.length);
        write(bytes);
        tryFlush();
    }

    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public void setLastError(Throwable ex) {
        lastError = ex;
    }
}

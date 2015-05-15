package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 11/05/15.
 */
public abstract class WebObjectSocket implements ObjectSocket {

    protected ArrayList objects = new ArrayList();
    protected FSTConfiguration conf;
    protected Throwable lastError;
    protected AtomicInteger sendSequence = new AtomicInteger(0); // defensive

    public AtomicInteger getSendSequence() {
        return sendSequence;
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        objects.add(toWrite);
        if (objects.size() > ActorClientConnector.OBJECT_MAX_BATCH_SIZE) {
            flush();
        }
    }

    public abstract void sendBinary(byte[] message);

    @Override
    public void flush() throws Exception {
        if (objects.size() == 0) {
            return;
        }
        objects.add(sendSequence.incrementAndGet()); // sequence
        Object[] objArr = objects.toArray();
        objects.clear();
        sendBinary(conf.asByteArray(objArr));
    }

    @Override
    public void setLastError(Throwable ex) {
        lastError = ex;
    }

    @Override
    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public void setConf(FSTConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
    }

}

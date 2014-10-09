package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MBPrinter;
import org.nustaq.serialization.minbin.MinBin;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by ruedi on 31.08.14.
 *
 * receive not threadsafe (must be single threaded)
 */
public abstract class WSAbstractObjectSocket implements ObjectSocket {

    protected FSTConfiguration conf;

    /**
     * its expected conf has special registrations such as Callback and remoteactor ref
     * @param conf
     */
    public WSAbstractObjectSocket(FSTConfiguration conf) {
        this.conf = conf;
    }

    protected byte nextRead[]; // fake as not polled
    public void setNextMsg(byte b[]) {
        nextRead = b;
    }

    @Override
    public Object readObject() throws Exception {
        if (nextRead == null)
            return null;
        final byte[] tmp = nextRead;
        nextRead = null;
        try {
            return conf.asObject(tmp);
        } catch (Exception e) {
            if ( conf.isCrossPlatform() ) {
                System.out.println("error reading:");
                MinBin.print(tmp);
            }
            throw e;
        }
    }

    @Override
    abstract public void writeObject(Object toWrite) throws Exception;

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void setLastError(Exception ex) {
    }

    @Override
    public void close() throws IOException {
    }

}

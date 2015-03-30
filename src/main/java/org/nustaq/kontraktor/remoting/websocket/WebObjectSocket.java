package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.coders.FSTMinBinDecoder;
import org.nustaq.serialization.minbin.MinBin;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Created by ruedi on 31.08.14.
 *
 */
public abstract class WebObjectSocket implements ObjectSocket {

    protected FSTConfiguration conf;

    /**
     * its expected conf has special registrations such as Callback and remoteactor ref
     * @param conf
     */
    public WebObjectSocket(FSTConfiguration conf) {
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
            final FSTObjectInput objectInput = conf.getObjectInput(tmp);
            final Object o = objectInput.readObject();
            // fixme debug code
            if (objectInput.getCodec() instanceof FSTMinBinDecoder) {
                FSTMinBinDecoder dec = (FSTMinBinDecoder) objectInput.getCodec();
                if (dec.getInputPos() != tmp.length) {
                    System.out.println("----- probably lost object --------- " + dec.getInputPos() + "," + tmp.length);
                    System.out.println(objectInput.readObject());
                }
            }
            return o;
        } catch (Exception e) {
            if ( conf.isCrossPlatform() ) {
                System.out.println("error reading:");
                MinBin.print(tmp);
            }
            throw e;
        }
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
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

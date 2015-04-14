package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.coders.FSTMinBinDecoder;
import org.nustaq.serialization.minbin.MinBin;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ruedi on 31.08.14.
 *
 * should be single writer
 *
 */
public abstract class WebObjectSocket implements ObjectSocket {

    protected FSTConfiguration conf;
    ArrayList toWrite = new ArrayList();

    /**
     * its expected conf has special registrations such as Callback and remoteactor ref serializers
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

    abstract protected void writeAndFlushObject(Object toWrite) throws Exception;

    @Override
    public void writeObject(Object o) throws Exception {
        synchronized (toWrite) {  // FIXME:
            toWrite.add(o);
        }
    }

    @Override
    public void flush() throws Exception {
        synchronized (toWrite) {
            int size = toWrite.size();
            if (size==0)
                return;
            if ( size == 1 ) {
                writeAndFlushObject(toWrite.get(0));
                toWrite.clear();
                return;
            }
            Object[] objects = toWrite.toArray();
            toWrite.clear();
            writeAndFlushObject(objects);
        }
    }

    @Override
    public void setLastError(Exception ex) {
    }

    @Override
    abstract public void close() throws IOException;

}

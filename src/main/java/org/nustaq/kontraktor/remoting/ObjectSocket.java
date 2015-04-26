package org.nustaq.kontraktor.remoting;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ruedi on 11.08.2014.
 */
public interface ObjectSocket {

    // blocking
    public Object readObject() throws Exception;
    // can be blocking
    public void writeObject(Object toWrite) throws Exception;

    public void flush() throws IOException, Exception;

    void setLastError(Exception ex);

    void close() throws IOException;

    public FSTConfiguration getConf();

    /**
     * used for seamless reconnection. Not all ObjectSockets support this (only FourK created WebObjectSockets for now)
     *
     * @param o
     */
    default void mergePendingWrites(ObjectSocket o) {
        throw new RuntimeException("unimplemented");
    }

    boolean isClosed();

}

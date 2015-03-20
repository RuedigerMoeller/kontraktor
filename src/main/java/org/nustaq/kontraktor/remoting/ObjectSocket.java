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
    // blocking
    public void writeObject(Object toWrite) throws Exception;

    public void flush() throws IOException;

    void setLastError(Exception ex);

    void close() throws IOException;

    public FSTConfiguration getConf();
}

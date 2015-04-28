package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

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

    boolean isClosed();

}

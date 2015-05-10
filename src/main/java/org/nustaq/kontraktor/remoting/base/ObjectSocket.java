package org.nustaq.kontraktor.remoting.base;

import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by moelrue on 5/7/15.
 */
public interface ObjectSocket {

    public void writeObject(Object toWrite) throws Exception;

    public void flush() throws Exception;

    public void setLastError(Throwable ex);

    public Throwable getLastError();

    public void setConf( FSTConfiguration conf );

    void close() throws IOException;

}

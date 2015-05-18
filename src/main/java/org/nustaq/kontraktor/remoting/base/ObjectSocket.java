package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.util.Log;
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

    /**
     * set by outer machinery
     * @param conf
     */
    public void setConf( FSTConfiguration conf );

    public FSTConfiguration getConf();

    void close() throws IOException;

    default boolean canWrite() {
        return true;
    }
}

package org.nustaq.kontraktor.remoting;

import java.io.IOException;

/**
 * Created by moelrue on 5/7/15.
 */
public interface WriteObjectSocket {

    public void writeObject(Object toWrite) throws Exception;

    public void flush() throws IOException, Exception;

    public void setLastError(Exception ex);

    void close() throws IOException;

}

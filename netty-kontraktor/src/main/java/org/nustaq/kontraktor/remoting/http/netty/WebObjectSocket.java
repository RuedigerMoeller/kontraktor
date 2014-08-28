package org.nustaq.kontraktor.remoting.http.netty;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.webserver.ClientSession;

import java.io.IOException;

/**
 * Created by ruedi on 28.08.14.
 */
public class WebObjectSocket implements ObjectSocket, ClientSession {

    FSTConfiguration conf;

    @Override
    public Object readObject() throws Exception {
        return null;
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {

    }

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

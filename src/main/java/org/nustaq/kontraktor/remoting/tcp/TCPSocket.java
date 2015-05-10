package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.remoting.OldObjectSocket;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.net.Socket;

/**
 * Created by ruedi on 11.08.2014.
 */
public class TCPSocket extends TCPObjectSocket implements OldObjectSocket {

    public TCPSocket(String host, int port) throws IOException {
        super(host, port);
    }

    public TCPSocket(String host, int port, FSTConfiguration conf) throws IOException {
        super(host, port, conf);
    }

    public TCPSocket(Socket socket, FSTConfiguration conf) throws IOException {
        super(socket, conf);
    }

    @Override
    public boolean isClosed() {
        return getSocket().isClosed();
    }

    @Override
    public void setLastError(Throwable ex) {

    }

    @Override
    public Throwable getLastError() {
        return null;
    }

    @Override
    public void setConf(FSTConfiguration conf) {

    }
}

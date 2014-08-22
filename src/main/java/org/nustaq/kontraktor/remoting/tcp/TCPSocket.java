package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 11.08.2014.
 */
public class TCPSocket extends TCPObjectSocket implements ObjectSocket {

    public TCPSocket(String host, int port) throws IOException {
        super(host, port);
    }

    public TCPSocket(String host, int port, FSTConfiguration conf) throws IOException {
        super(host, port, conf);
    }

    public TCPSocket(Socket socket, FSTConfiguration conf) throws IOException {
        super(socket, conf);
    }
}

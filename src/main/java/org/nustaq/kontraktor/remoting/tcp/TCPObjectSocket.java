package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by ruedi on 11.08.2014.
 */
public class TCPObjectSocket implements ObjectSocket {

    InputStream in;
    OutputStream out;
    FSTConfiguration conf;
    Socket socket;
    Exception lastErr;
    boolean stopped;

    public TCPObjectSocket(InputStream in, OutputStream out, Socket socket, FSTConfiguration conf) {
        this.in = in;
        this.out = out;
        this.conf = conf;
        this.socket = socket;
    }

    public Exception getLastErr() {
        return lastErr;
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public Object readObject() throws Exception {
        int ch1 = (in.read() + 256) & 0xff;
        int ch2 = (in.read()+ 256) & 0xff;
        int ch3 = (in.read() + 256) & 0xff;
        int ch4 = (in.read() + 256) & 0xff;
        int len = (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
        int orglen = len;
        byte buffer[] = new byte[len]; // this could be reused !
        while (len > 0)
            len -= in.read(buffer, buffer.length - len, len);
        try {
            return conf.getObjectInput(buffer).readObject();
        } catch (Exception e) {
            System.out.println("orglen: "+orglen);
            throw e;
        }
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
        objectOutput.writeObject(toWrite);

        int written = objectOutput.getWritten();
        out.write((written >>> 0) & 0xFF);
        out.write((written >>> 8) & 0xFF);
        out.write((written >>> 16) & 0xFF);
        out.write((written >>> 24) & 0xFF);

        out.write(objectOutput.getBuffer(), 0, written);
        objectOutput.flush();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void setLastError(Exception ex) {
        stopped = true;
        lastErr = ex;
    }

}

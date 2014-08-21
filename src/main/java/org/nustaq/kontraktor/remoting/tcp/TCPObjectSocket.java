package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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

    AtomicBoolean readLock = new AtomicBoolean(false);
    AtomicBoolean writeLock = new AtomicBoolean(false);

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
        try {
            while ( !readLock.compareAndSet(false,true) );
//            int tag = in.read();
            int ch1 = (in.read() + 256) & 0xff;
            int ch2 = (in.read()+ 256) & 0xff;
            int ch3 = (in.read() + 256) & 0xff;
            int ch4 = (in.read() + 256) & 0xff;
            int len = (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
//            int tag1 = in.read();

//            if ( tag != 77 || tag1!= tag ) {
//                throw new RuntimeException("ERROR!");
//            }
            if ( len <= 0 )
                throw new EOFException("client closed");
            int orglen = len;
            byte buffer[] = new byte[len]; // this could be reused !
    //        System.out.println("LEN:"+len);
            while (len > 0)
                len -= in.read(buffer, buffer.length - len, len);
            try {
                return conf.getObjectInput(buffer).readObject();
            } catch (Exception e) {
                System.out.println("orglen: "+orglen+" "+new String(buffer,0));
                final Object retry = conf.getObjectInput(buffer).readObject();
                throw e;
            }
        } finally {
            readLock.set(false);
        }
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        try {
            while ( !writeLock.compareAndSet(false,true) );
            FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
            objectOutput.writeObject(toWrite);

            int written = objectOutput.getWritten();
//            out.write(77); // debug tag
            out.write((written >>> 0) & 0xFF);
            out.write((written >>> 8) & 0xFF);
            out.write((written >>> 16) & 0xFF);
            out.write((written >>> 24) & 0xFF);
//            out.write(77);

            out.write(objectOutput.getBuffer(), 0, written);
            objectOutput.flush();
        } finally {
            writeLock.set(false);
        }
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

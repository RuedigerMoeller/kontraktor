package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.coders.FSTMinBinDecoder;
import org.nustaq.serialization.minbin.MinBin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 31.08.14.
 *
 * An object socket operating via websockets. It employs sequencing in order to guarantee message delivery
 * and also enable seamless reconnection after connection loss + long poll fallback impl.
 *
 */
public abstract class WebObjectSocket implements ObjectSocket {

    protected FSTConfiguration conf;
    protected ArrayList toWrite = new ArrayList();
    protected AtomicInteger writeSequence = new AtomicInteger(1);
    protected AtomicInteger readSequence = new AtomicInteger(0);

    /**
     * its expected conf has special registrations such as Callback and remoteactor ref serializers
     * @param conf
     */
    public WebObjectSocket(FSTConfiguration conf) {
        this.conf = conf;
    }

    protected byte nextRead[]; // fake as not polled
    protected Object receiveHistory[][] = new Object[16][];
    protected int mask = receiveHistory.length-1;
    public void setNextMsg(byte b[]) {
        nextRead = b;
    }


    @Override
    public Object readObject() throws Exception {
        if (nextRead == null)
            return null;
        final byte[] tmp = nextRead;
        nextRead = null;
        try {
            final FSTObjectInput objectInput = conf.getObjectInput(tmp);
            Object readO = objectInput.readObject();
            if ( readO instanceof Object[] == false ) {
                return readO; // bypass sequencing completely for single messages
            }
            Object o[] = (Object[]) readO;
            // fixme debug code
            if (objectInput.getCodec() instanceof FSTMinBinDecoder) {
                FSTMinBinDecoder dec = (FSTMinBinDecoder) objectInput.getCodec();
                if (dec.getInputPos() != tmp.length) {
                    System.out.println("----- probably lost object --------- " + dec.getInputPos() + "," + tmp.length);
                    System.out.println(objectInput.readObject());
                }
            }
            int sequence = (int) o[o.length-1];
            int curSeq = readSequence.get();
            if ( curSeq == 0 ) {
                if ( ! readSequence.compareAndSet(0,sequence) ) {
                    throw new RuntimeException("unexpected multithreading");
                }
            } else {
                if ( curSeq != sequence-1 ) {
                    if ( sequence-curSeq > mask) {
                        close();
                        return null;
                    }
                    receiveHistory[sequence&mask] = o;
//                    System.out.println("sequence GAP detected received: "+sequence+" current:"+ curSeq +" "+getClass().getSimpleName()+" saved in "+(sequence&mask));
                    return RemoteRefRegistry.OUT_OF_ORDER_SEQ;
                } else {
                    if ( ! readSequence.compareAndSet(sequence-1,sequence) ) {
                        throw new RuntimeException("unexpected multithreading 1");
                    }
                    Object[] next = receiveHistory[(sequence+1) & mask];
                    while ( next != null && ((int)next[next.length-1]) == sequence+1 ) {
                        Object[] newO = new Object[next.length+o.length-1];
                        System.arraycopy(o,0,newO,0,o.length-1);
                        System.arraycopy(next,0,newO,o.length-1,next.length);
                        o = newO;
                        receiveHistory[(sequence + 1) & mask] = null;
                        sequence++;
                        if ( ! readSequence.compareAndSet(sequence-1,sequence) ) {
                            throw new RuntimeException("unexpected multithreading 1");
                        }
                        next = receiveHistory[(sequence + 1) & mask];
                    }
                }
            }
            return o;
        } catch (Exception e) {
            if ( conf.isCrossPlatform() ) {
                System.out.println("error reading:");
                MinBin.print(tmp);
            }
            throw e;
        }
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
    }

    abstract protected void writeAndFlushObject(Object toWrite) throws Exception;

    @Override
    public void writeObject(Object o) throws Exception {
        synchronized (toWrite) {  // FIXME:
            toWrite.add(o);
            System.out.println("writing to socket Q siz:" + toWrite.size());
        }
    }

    @Override
    public void flush() throws Exception {
        synchronized (toWrite) {
            int size = toWrite.size();
            if (size==0)
                return;
            if ( ! isClosed() ) {
                toWrite.add(writeSequence.getAndIncrement());
                Object[] objects = toWrite.toArray(); // fixme performance
                toWrite.clear();
                writeAndFlushObject(objects);
            }
        }
    }

    /**
     * handover my object socket to a (cached) registry. To get this atomic, also update the ref here
     */
    public void mergePendingWritesAndReplaceInRef(ActorServerAdapter.ActorServerConnection oldRegistry) {
        WebObjectSocket discardedOS = (WebObjectSocket) oldRegistry.getObjectSocket().get();
        // note: can only be done right after construction when this socket is not exposed to
        // other threads !!
        synchronized (discardedOS.toWrite) {
            toWrite = discardedOS.toWrite;
            oldRegistry.getObjectSocket().set(this);
            // replace registry in adapter
            getChannel().setAttribute("con", oldRegistry);
        }
    }

    public abstract WebSocketChannelAdapter getChannel();

    public AtomicInteger getWriteSequence() {
        return writeSequence;
    }

    public AtomicInteger getReadSequence() {
        return readSequence;
    }

    @Override
    public void setLastError(Exception ex) {
    }

    @Override
    abstract public void close() throws IOException;

}

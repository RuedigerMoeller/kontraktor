package org.nustaq.kontraktor.remoting;

import org.nustaq.serialization.FSTConfiguration;

/**
 * Created by ruedi on 11.08.2014.
 */
public interface OldObjectSocket extends org.nustaq.kontraktor.remoting.base.ObjectSocket {

    // blocking
    public Object readObject() throws Exception;
    public FSTConfiguration getConf();

    boolean isClosed();

}

package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by ruedi on 11.08.2014.
 */
public interface ObjectSocket extends WriteObjectSocket {

    // blocking
    public Object readObject() throws Exception;
    public FSTConfiguration getConf();

    boolean isClosed();

}

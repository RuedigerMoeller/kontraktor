package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

/**
 * Created by ruedi on 13.03.17.
 */
public interface Forwarder {

    boolean forward(RemoteCallEntry read);

}

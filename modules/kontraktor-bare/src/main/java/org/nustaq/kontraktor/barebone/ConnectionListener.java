package org.nustaq.kontraktor.barebone;

/**
 * Created by ruedi on 06/06/15.
 */
public interface ConnectionListener {

    /**
     * connection has closed, either because the server was not reachable or
     * unrecoverable message loss has occured.
     * @param s
     */
    public void connectionClosed(String s);

}

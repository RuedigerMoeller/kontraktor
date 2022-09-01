package org.nustaq.kontraktor.remoting.base;

public interface TrafficMonitor {

    /**
     * Will be called from request receiver
     * @param size bytes count from request content length
     * @param sid user session identifier, can be null
     * @param path request path, can be null
     */
    void requestReceived(int size, String sid, String path);

    /**
     * Will be called from response sender
     * @param size bytes count from response content length
     * @param sid user session identifier, can be null
     * @param path request path, can be null
     */
    void responseSend(int size, String sid, String path);
}

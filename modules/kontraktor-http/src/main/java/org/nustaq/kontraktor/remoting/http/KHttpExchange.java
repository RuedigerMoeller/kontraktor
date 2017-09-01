package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.util.Pair;

/**
 * Created by ruedi on 19.06.17.
 *
 * wrapper to encapsulate server specific http implementation/behaviour
 *
 */
public interface KHttpExchange {
    void endExchange();

    void setResponseContentLength(int length);

    void setResponseCode(int i);

    void send(String s);

    void send(byte[] b);

    void sendAuthResponse(byte[] response, String sessionId);

}

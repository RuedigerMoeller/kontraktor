package org.nustaq.kontraktor.remoting.base;

import java.io.Serializable;

public class Reconnect implements Serializable {
    String sessionId;
    long remoteRefId;

    public Reconnect(String sessionId, long remoteRefId) {
        this.sessionId = sessionId;
        this.remoteRefId = remoteRefId;
    }

    public long getRemoteRefId() {
        return remoteRefId;
    }

    public String getSessionId() {
        return sessionId;
    }
}

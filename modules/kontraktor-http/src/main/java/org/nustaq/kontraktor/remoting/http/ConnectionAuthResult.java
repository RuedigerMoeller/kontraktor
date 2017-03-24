package org.nustaq.kontraktor.remoting.http;

/**
 * Created by ruedi on 24.03.17.
 */
public class ConnectionAuthResult {
    String sid;
    String error;

    public ConnectionAuthResult(String sid, String error) {
        this.sid = sid;
        this.error = error;
    }

    public String getSid() {
        return sid;
    }

    public String getError() {
        return error;
    }

    public boolean isError() {
        return error != null || sid == null;
    }
}


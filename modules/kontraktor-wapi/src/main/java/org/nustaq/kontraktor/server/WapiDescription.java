package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.Actor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ruedi on 09.03.17.
 */
public class WapiDescription implements Serializable {

    private String name;
    private long lastPing;
    private long timeout = 15_000L;
    private String uniqueKey;
    private String version = "1";
    transient Actor remoteRef;

    public WapiDescription() {
        uniqueKey = UUID.randomUUID().toString();
        receiveHeartbeat();
    }

    public String getVersion() {
        return version;
    }

    public WapiDescription name(final String name) {
        this.name = name;
        return this;
    }

    public WapiDescription timeout(final long timeout) {
        this.timeout = timeout;
        return this;
    }

    public Actor getRemoteRef() {
        return remoteRef;
    }

    public WapiDescription remoteRef(final Actor remoteRef) {
        this.remoteRef = remoteRef;
        return this;
    }

    public String getName() {
        return name;
    }

    public void receiveHeartbeat() {
        lastPing = System.currentTimeMillis();
    }

    public long getLastPing() {
        return lastPing;
    }

    public boolean hasTimedOut() {
        return System.currentTimeMillis()-lastPing > timeout;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    @Override
    public String toString() {
        return "WapiDescription{" +
            "name='" + name + '\'' +
            ", timeout=" + timeout +
            ", uniqueKey='" + uniqueKey + '\'' +
            ", version='" + version + '\'' +
            '}';
    }
}

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ruedi on 11.08.2015.
 */
public class ServiceDescription implements Serializable {

    String name;
    String description;
    String uniqueKey = UUID.randomUUID().toString();
    ConnectableActor connectable;
    transient long lastPing;
    long timeout = 100_000L; // TODO: dev-setting needs to be replaced for production mode

    public ServiceDescription(String name) {
        this.name = name;
    }

    public ServiceDescription description(final String description) {
        this.description = description;
        return this;
    }

    public ServiceDescription connectable(final ConnectableActor connectable) {
        this.connectable = connectable;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ServiceDescription timeout(final long timeout) {
        this.timeout = timeout;
        return this;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public ConnectableActor getConnectable() {
        return connectable;
    }

    public ServiceDescription unqiqueKey(final String unqiqueKey) {
        this.uniqueKey = unqiqueKey;
        return this;
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

    @Override
    public String toString() {
        return "ServiceDescription{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", uniqueKey='" + uniqueKey + '\'' +
                ", connectable=" + connectable +
                ", lastPing=" + lastPing +
                ", timeout=" + timeout +
                '}';
    }

    public Class<? extends Actor> getActorClazz() {
        if ( connectable == null )
            return null;
        return connectable.getActorClass();
    }
}

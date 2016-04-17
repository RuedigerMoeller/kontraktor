package org.nustaq.kluster.processes;

import java.io.Serializable;

/**
 * Created by ruedi on 16/04/16.
 */
public class StarterDesc implements Serializable {

    String id;
    String host;
    ProcessStarter remoteRef;
    int port;
    String name;

    public String getName() {
        return name;
    }

    public StarterDesc name(final String name) {
        this.name = name;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public ProcessStarter getRemoteRef() {
        return remoteRef;
    }

    public StarterDesc id(final String id) {
        this.id = id;
        return this;
    }

    public StarterDesc host(final String host) {
        this.host = host;
        return this;
    }

    public StarterDesc port(final int port) {
        this.port = port;
        return this;
    }

    public StarterDesc remoteRef(final ProcessStarter remoteRef) {
        this.remoteRef = remoteRef;
        return this;
    }

    @Override
    public String toString() {
        return "StarterDesc{" +
            "id='" + id + '\'' +
            ", host='" + host + '\'' +
            ", remoteRef=" + remoteRef +
            ", port=" + port +
            ", name='" + name + '\'' +
            '}';
    }

    public int getPort() {
        return port;
    }
}

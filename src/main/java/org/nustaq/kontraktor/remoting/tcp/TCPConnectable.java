/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.function.Consumer;

/**
 * Created by ruedi on 19/05/15.
 *
 * Describes a connectable remote actor
 *
 */
public class TCPConnectable implements ConnectableActor {

    String host;
    int port;
    Class actorClz;
    Coding coding = new Coding(SerializerType.FSTSer);
    int inboundQueueSize = SimpleScheduler.DEFQSIZE;

    public TCPConnectable() {
    }

    /**
     *
     * @param host - ip/host e.g. "192.168.4.5"
     * @param port - port
     * @param actorClz - actor clazz to connect to
     */
    public TCPConnectable(Class actorClz, String host, int port) {
        this.host = host;
        this.port = port;
        this.actorClz = actorClz;
    }

    @Override
    public <T extends Actor> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback, Consumer<Actor> actorDisconnecCB) {
        if ( actorClz == null )
            throw new RuntimeException("actor class cannot be null on TCPConnectable");
        if ( host == null )
            throw new RuntimeException("host cannot be null on TCPConnectable");
        Promise result = new Promise();
        Runnable connect = () -> {
            TCPClientConnector client = new TCPClientConnector(port,host,disconnectCallback);
            ActorClient connector = new ActorClient(client,actorClz,coding);
            connector.connect(inboundQueueSize, actorDisconnecCB).then(result);
        };
        if ( ! Actor.inside() ) {
            TCPClientConnector.get().execute(() -> Thread.currentThread().setName("tcp singleton remote client actor polling"));
            TCPClientConnector.get().execute(connect);
        }
        else
            connect.run();
        return result;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Class getActorClz() {
        return actorClz;
    }

    public TCPConnectable host(String host) {
        this.host = host;
        return this;
    }

    public TCPConnectable port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public TCPConnectable actorClass(Class actorClz) {
        this.actorClz = actorClz;
        return this;
    }

    @Override
    public Class<? extends Actor> getActorClass() {
        return actorClz;
    }

    public TCPConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public TCPConnectable serType(SerializerType sertype) {
        this.coding = new Coding(sertype);
        return this;
    }

    /**
     * default is 32k (SimpleScheduler.DEFQSIZE)
     * @param inboundQueueSize
     * @return
     */
    public TCPConnectable inboundQueueSize(final int inboundQueueSize) {
        this.inboundQueueSize = inboundQueueSize;
        return this;
    }

    @Override
    public String getKey() {
        return actorClz.getName()+host+port+coding;
    }

    @Override
    public String toString() {
        return "TCPConnectable{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", actorClz=" + actorClz +
            ", coding=" + coding +
            ", inboundQueueSize=" + inboundQueueSize +
            '}';
    }
}

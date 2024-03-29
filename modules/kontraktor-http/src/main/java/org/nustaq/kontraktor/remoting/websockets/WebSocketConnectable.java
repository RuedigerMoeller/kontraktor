/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

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

package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Created by ruedi on 19/05/15.
 *
 * describes a remotactor connectable via websockets
 *
 */
public class WebSocketConnectable implements ConnectableActor {

    Class actorClass;
    String url;
    Coding coding = new Coding(SerializerType.FSTSer);
    int inboundQueueSize = SimpleScheduler.DEFQSIZE;

    public WebSocketConnectable() {}

    public WebSocketConnectable(Class clz, String url) {
        this.actorClass = clz;
        this.url = url;
    }

    public WebSocketConnectable url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public <T extends Actor> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback, Consumer<Actor> actorDisconnecCB) {
        if ( actorClass == null ) {
            throw new RuntimeException("pls specify actor clazz to connect to");
        }
        Promise result = new Promise();
        Runnable connect = () -> {
            JSR356ClientConnector client = null;
            try {
                client = new JSR356ClientConnector(url);
                ActorClient connector = new ActorClient(client, actorClass,coding);
                connector.connect(inboundQueueSize, actorDisconnecCB).then(result);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                result.reject(e);
            }
        };
        if ( ! Actor.inside() ) {
            JSR356ClientConnector.get().execute(() -> Thread.currentThread().setName("JSR356WS singleton remote client actor polling"));
            JSR356ClientConnector.get().execute(connect);
        }
        else
            connect.run();
        return result;
    }

    @Override
    public WebSocketConnectable actorClass(Class actorClz) {
        actorClass = actorClz;
        return this;
    }

    @Override
    public Class<? extends Actor> getActorClass() {
        return actorClass;
    }

    public WebSocketConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public WebSocketConnectable serType(SerializerType sertype) {
        this.coding = new Coding(sertype);
        return this;
    }

    public Class getClz() {
        return actorClass;
    }

    public String getUrl() {
        return url;
    }

    public Coding getCoding() {
        return coding;
    }

    public WebSocketConnectable inboundQueueSize(final int inboundQSize) {
        this.inboundQueueSize = inboundQSize;
        return this;
    }

    @Override
    public String getKey() {
        return actorClass.getName()+url+coding;
    }

    @Override
    public String toString() {
        return "WebSocketConnectable{" +
                "actorClass=" + actorClass +
                ", url='" + url + '\'' +
                ", coding=" + coding +
                '}';
    }
}

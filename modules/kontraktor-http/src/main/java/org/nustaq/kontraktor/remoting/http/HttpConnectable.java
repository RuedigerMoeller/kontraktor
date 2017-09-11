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

package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;

import java.util.function.Consumer;

/**
 * Created by ruedi on 19/05/15.
 *
 * Default configuration is Long Poll, Binary Serialization
 * example:
 * <pre>
 * remoteApp = (MyHttpApp)
 *             new HttpConnectable(MyHttpApp.class, "http://localhost:8080/api")
 *                 .serType(SerializerType.JsonNoRefPretty)
 *                 .connect(null)
 *                 .await();
 * </pre>
 *
 */
public class HttpConnectable implements ConnectableActor {

    protected Class actorClz;
    protected String url;
    protected Coding coding = new Coding( SerializerType.FSTSer );
    protected Object[] authData; // always json encoded
    protected boolean noPoll = false;

    protected boolean shortPollMode = false;   // if true, do short polling instead
    protected long shortPollIntervalMS = 5000;
    protected int inboundQueueSize = SimpleScheduler.DEFQSIZE;

    public HttpConnectable() {
    }

    public HttpConnectable(Class clz, String url) {
        this.actorClz = clz;
        this.url = url;
    }

    public HttpConnectable noPoll(boolean noPoll) {
        this.noPoll = noPoll;
        return this;
    }

    public HttpConnectable shortPoll(boolean shortPollMode) {
        this.shortPollMode = shortPollMode;
        return this;
    }

    public HttpConnectable shortPollIntervalMS(long shortPollIntervalMS) {
        this.shortPollIntervalMS = shortPollIntervalMS;
        return this;
    }

    public HttpConnectable actorClazz(Class clz) {
        this.actorClz = clz;
        return this;
    }

    public HttpConnectable url(String url) {
        this.url = url;
        return this;
    }

    public HttpConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    /**
     * overwrites settings made by 'coding'
     * @param type
     * @return
     */
    public HttpConnectable serType(SerializerType type) {
        this.coding = new Coding(type);
        return this;
    }

    public HttpConnectable authData(Object[] authData) {
        this.authData = authData;
        return this;
    }

    @Override
    public <T extends Actor> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback, Consumer<Actor> actorDisconnecCB) {
        HttpClientConnector con = new HttpClientConnector(this);
        con.disconnectCallback = disconnectCallback;
        ActorClient actorClient = new ActorClient(con, actorClz, coding);
        Promise p = new Promise();
        con.getRefPollActor().execute(() -> {
            Thread.currentThread().setName("Http Ref Polling");
            actorClient.connect(inboundQueueSize, null).then(p);
        });
        return p;
    }

    @Override
    public ConnectableActor actorClass(Class actorClz) {
        this.actorClz = actorClz;
        return this;
    }

    @Override
    public Class<? extends Actor> getActorClass() {
        return actorClz;
    }

    public Class getActorClz() {
        return actorClz;
    }

    public String getUrl() {
        return url;
    }

    public Coding getCoding() {
        return coding;
    }

    public Object[] getAuthData() {
        return authData;
    }

    public boolean isNoPoll() {
        return noPoll;
    }

    public boolean isShortPollMode() {
        return shortPollMode;
    }

    public long getShortPollIntervalMS() {
        return shortPollIntervalMS;
    }

    public HttpConnectable inboundQueueSize(final int inboundQSize) {
        this.inboundQueueSize = inboundQSize;
        return this;
    }
}

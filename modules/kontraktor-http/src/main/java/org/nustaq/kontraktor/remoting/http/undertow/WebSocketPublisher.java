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

package org.nustaq.kontraktor.remoting.http.undertow;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.base.TrafficMonitor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;

import java.util.function.Consumer;

/**
 * Created by ruedi on 04/06/15.
 */
public class WebSocketPublisher implements ActorPublisher {

    BldFourK cfg; // used in cfgbuilder

    String hostName;
    String urlPath;
    int port;
    Coding coding = new Coding(SerializerType.FSTSer);
    Actor facade;
    boolean sendStringMessages = false;
    boolean sendSid = false;
    private TrafficMonitor trafficMonitor;

    public WebSocketPublisher() {}

    public WebSocketPublisher(Actor facade, String host, String path, int port) {
        this.hostName = host;
        this.urlPath = path;
        this.port = port;
        this.facade = facade;
    }

    public WebSocketPublisher(BldFourK cfgFourK, Actor facade, String hostName, String urlPath, int port) {
        this(facade,hostName,urlPath,port);
        this.cfg = cfgFourK;
    }

    public BldFourK buildWebsocket() {
        return cfg;
    }

    @Override
    public IPromise<ActorServer> publish(Consumer<Actor> disconnectCallback) {
        Promise finished = new Promise();
        try {
            ActorServer publisher = new ActorServer(
                new UndertowWebsocketServerConnector(urlPath,port,hostName).sendStrings(sendStringMessages).sendSid(sendSid),
                facade,
                coding
            );
            facade.execute(() -> {
                try {
                    publisher.start(disconnectCallback);
                    finished.resolve(publisher);
                } catch (Exception e) {
                    finished.reject(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise(null,e);
        }
        return finished;
    }

    public WebSocketPublisher hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public WebSocketPublisher urlPath(String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public WebSocketPublisher port(int port) {
        this.port = port;
        return this;
    }

    public WebSocketPublisher coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public WebSocketPublisher serType( SerializerType tp ) {
        return coding( new Coding( tp ) );
    }

    public WebSocketPublisher facade(Actor facade) {
        this.facade = facade;
        return this;
    }

    @Override
    public void setTrafficMonitor(final TrafficMonitor trafficMonitor) {
        this.trafficMonitor = trafficMonitor;
    }

    /**
     * node.js does not support full file api, so binary messages cannot be de'jsoned. Add an
     * option to send all data as String via websocket (FIXME: quite some overhead as byte array is UTF-8'ed)
     * default is binary messages (ok for browsers, not node)
     * @param sendStringMessages
     * @return
     */
    public WebSocketPublisher sendStringMessages(final boolean sendStringMessages) {
        this.sendStringMessages = sendStringMessages;
        return this;
    }

    public WebSocketPublisher cfg(BldFourK cfg) {
        this.cfg = cfg;
        return this;
    }

    /**
     * send sessionid like sid:234-234-234 after a client connects. WARNING: only javascript-SPA clients
     * currently understand this. ensure false for non-SPA servers. Defaults false
     *
     */
    public WebSocketPublisher sendSid(boolean sendSid) {
        this.sendSid = sendSid;
        return this;
    }

    public TrafficMonitor getTrafficMonitor() {
        return trafficMonitor;
    }
}

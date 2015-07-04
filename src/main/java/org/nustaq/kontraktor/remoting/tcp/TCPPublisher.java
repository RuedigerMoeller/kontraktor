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
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.function.Consumer;

/**
 * Created by ruedi on 18/06/15.
 */
public class TCPPublisher extends TCPNIOPublisher {

    public TCPPublisher() {
        super();
    }

    public TCPPublisher(Actor facade, int port) {
        super(facade, port);
    }

    @Override
    public IPromise<ActorServer> publish(Consumer<Actor> disconnectCallback) {
        return TCPServerConnector.Publish(facade,port,coding,disconnectCallback);
    }

    @Override
    public TCPPublisher serType(SerializerType type) {
        return (TCPPublisher) super.serType(type);
    }

    @Override
    public TCPPublisher facade(Actor facade) {
        return (TCPPublisher) super.facade(facade);
    }

    @Override
    public TCPPublisher port(int port) {
        return (TCPPublisher) super.port(port);
    }

    @Override
    public TCPPublisher coding(Coding coding) {
        return (TCPPublisher) super.coding(coding);
    }

    @Override
    public Actor getFacade() {
        return super.getFacade();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    public Coding getCoding() {
        return super.getCoding();
    }
}

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
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.ObjectAsyncSocketConnection;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.Coding;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 *
 * Publishes an actor as a server using non-blocking IO backed TCP.
 * The number of threads does not increase with the number of clients.
 *
 */
public class NIOServerConnector extends AsyncServerSocket implements ActorServerConnector {

    public static IPromise<ActorServer> Publish(Actor facade, int port, Coding coding) {
        return Publish(facade,port,coding,null);
    }

    public static Promise<ActorServer> Publish(Actor facade, int port, Coding coding, Consumer<Actor> disconnectHandler) {
        Promise finished = new Promise();
        try {
            ActorServer publisher = new ActorServer(new NIOServerConnector(port), facade, coding);
            facade.execute(() -> {
                try {
                    publisher.start(disconnectHandler);
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

    int port;

    public NIOServerConnector(int port) {
        super();
        this.port = port;
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        connect( port, (key,channel) -> {
            MyObjectAsyncSocketConnection sc = new MyObjectAsyncSocketConnection(key,channel);
            ObjectSink sink = factory.apply(sc);
            sc.init(sink);
            return sc;
        });
    }

    @Override
    public IPromise closeServer() {
        try {
            super.close();
        } catch (IOException e) {
            return new Promise<>(null,e);
        }
        return new Promise<>(null);
    }

    static class MyObjectAsyncSocketConnection extends ObjectAsyncSocketConnection {
        ObjectSink sink;
        String id;

        public MyObjectAsyncSocketConnection(SelectionKey key, SocketChannel chan) {
            super(key, chan);
            this.id = UUID.randomUUID().toString();
        }

        public void init( ObjectSink sink ) { this.sink = sink; }

        @Override public void receivedObject(Object o) { sink.receiveObject(o, null, null ); }

        @Override
        public void closed(Throwable ioe) {
            isClosed = true;
            sink.sinkClosed();
        }

        public void close() throws IOException {
            chan.close();
            sink.sinkClosed();
        }

        @Override
        public String getConnectionIdentifier() {
            return id;
        }

    }
}

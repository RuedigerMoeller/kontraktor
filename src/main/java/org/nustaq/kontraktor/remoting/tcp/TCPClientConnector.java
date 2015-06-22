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

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.serialization.util.FSTUtil;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public class TCPClientConnector implements ActorClientConnector {

    public static class RemotingHelper extends Actor<RemotingHelper> {}
    protected static AtomicReference<RemotingHelper> singleton =  new AtomicReference<>();

    /**
     * in case clients are connected from non actor world, provide a global actor(thread) for remote client processing
     * (=polling queues, encoding)
     */
    protected static RemotingHelper get() {
        synchronized (singleton) {
            if ( singleton.get() == null ) {
                singleton.set(Actors.AsActor(RemotingHelper.class));
            }
            return singleton.get();
        }
    }

    protected int port;
    protected String host;
    protected MyTCPSocket socket;
    protected Callback<ActorClientConnector> disconnectCallback;

    public TCPClientConnector(int port, String host, Callback<ActorClientConnector> disconnectCallback) {
        this.port = port;
        this.host = host;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public IPromise connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        Promise res = new Promise();
        socket = new MyTCPSocket(host,port);
        ObjectSink sink = factory.apply(socket);
        new Thread(() -> {
            res.resolve();
            while (!socket.isClosed()) {
                try {
                    Object o = socket.readObject();
                    sink.receiveObject(o, null);
                } catch (Exception e) {
                    if (e instanceof EOFException == false && e instanceof SocketException == false )
                        Log.Warn(this, e);
                    else {
                        Log.Warn( this, e.getMessage() );
                    }
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        Log.Warn(this, e.getMessage());
                    }
                }
            }
            if ( disconnectCallback != null ) {
                disconnectCallback.complete(this,null);
            }
            sink.sinkClosed();
        }, "tcp client receiver").start();
        return res;
    }

    @Override
    public IPromise closeClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new Promise<>(e);
        }
        return new Promise<>();
    }

    static class MyTCPSocket extends TCPObjectSocket implements ObjectSocket {

        ArrayList objects = new ArrayList();

        public MyTCPSocket(String host, int port) throws IOException {
            super(host, port);
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            objects.add(toWrite);
            if (objects.size()>OBJECT_MAX_BATCH_SIZE) {
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if ( objects.size() == 0 ) {
                return;
            }
            objects.add(0); // sequence
            Object[] objArr = objects.toArray();
            objects.clear();

            try {
                super.writeObject(objArr);
            } catch (Exception e) {
                FSTUtil.<RuntimeException>rethrow(e);
            }

            super.flush();
        }
    }

}

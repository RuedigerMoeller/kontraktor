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

import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 11/05/15.
 *
 * implements batching on a message-object level. This results in nice speed up and protocol compression,
 * as serializing a batch of messages automatically strips double objects+strings.
 *
 */
public abstract class WebObjectSocket implements ObjectSocket {

    protected List objects;
    protected FSTConfiguration conf;
    protected Throwable lastError;
    protected AtomicInteger sendSequence = new AtomicInteger(0); // defensive
    protected volatile boolean isClosed;

    public AtomicInteger getSendSequence() {
        return sendSequence;
    }

    public WebObjectSocket() {
        objects = new ArrayList();
    }

    Thread debugT; boolean DBGTHREADS = false;
    @Override
    public void writeObject(Object toWrite) throws Exception {
        if ( DBGTHREADS ) {
            if (debugT != null && debugT != Thread.currentThread()) {
                System.out.println("Thread " + Thread.currentThread().getName() + " other " + debugT.getName());
                System.out.println("writing object:" + toWrite);
                if (Thread.currentThread().getName().indexOf("Dispatch") < 0) {
                    int debug = 1;
                }
                debugT = Thread.currentThread();
            }
            debugT = Thread.currentThread();
        }
        synchronized (this) { //FIXME: hotfix. Didn't do all the single thread enforcement to introduce this ..
            objects.add(toWrite);
            if (objects.size() > getObjectMaxBatchSize()) {
                flush();
            }
        }
    }

    protected int getObjectMaxBatchSize() {
        return ActorClientConnector.OBJECT_MAX_BATCH_SIZE;
    }

    public abstract void sendBinary(byte[] message);

    @Override
    public void flush() throws Exception {
        if (DBGTHREADS) {
            if (debugT != null && debugT != Thread.currentThread()) {
                System.out.println("flush Thread " + Thread.currentThread().getName() + " other " + debugT.getName());
                if (Thread.currentThread().getName().indexOf("Dispatch") < 0) {
                    int debug = 1;
                }
                debugT = Thread.currentThread();
            }
        }
        synchronized (this) { //FIXME: hotfix. Didn't do all the single thread enforcement to introduce this ..
            if (objects.size() == 0) {
                return;
            }
            if (isClosed()) {
                if (lastError != null) {
                    FSTUtil.<RuntimeException>rethrow(lastError);
                } else {
                    throw new IOException("WebSocket is closed");
                }
            }
            objects.add(sendSequence.incrementAndGet()); // sequence
            Object[] objArr = objects.toArray();
            objects.clear();
            sendBinary(conf.asByteArray(objArr));
        }
    }

    @Override
    public void setLastError(Throwable ex) {
        lastError = ex;
    }

    @Override
    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public void setConf(FSTConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public FSTConfiguration getConf() {
        return conf;
    }

    public boolean isClosed() {
        return isClosed;
    }

}

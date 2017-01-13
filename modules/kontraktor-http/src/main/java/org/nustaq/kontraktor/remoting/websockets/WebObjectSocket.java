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
import org.nustaq.kontraktor.util.Log;
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
    protected SendStatistics stats;
    protected int maxMsgSize;
    protected int maxMsgSizeSeen;
    
    public interface SendStatistics {
        void updateBytesSent(int bytesSent);
        void updateLatency(int latency);
    }

    public SendStatistics getStats() {
        return stats;
    }

    public void setStats(SendStatistics stats) {
        this.stats = stats;
    }

    public AtomicInteger getSendSequence() {
        return sendSequence;
    }

    public WebObjectSocket() {
        objects = new ArrayList();
    }

    @Override
    public void writeObject(Object toWrite) throws Exception {
        objects.add(toWrite);
        if (objects.size() > getObjectMaxBatchSize()) {
            flush();
        }
    }

    protected int getObjectMaxBatchSize() {
        return ActorClientConnector.OBJECT_MAX_BATCH_SIZE;
    }

    public abstract void sendBinary(byte[] message);

    @Override
    public void flush() throws Exception {
        if (objects.size() == 0) {
            return;
        }
        if ( isClosed() ) {
            if ( lastError != null ) {
                FSTUtil.<RuntimeException>rethrow(lastError);
            } else {
                throw new IOException("WebSocket is closed");
            }
        }
        int seqNr = sendSequence.incrementAndGet();
        objects.add(seqNr); // sequence
        Object[] objArr = objects.toArray();
        byte[] asByteArray = conf.asByteArray(objArr);
        int bytesSent = asByteArray.length;
        if (maxMsgSize > 0 && bytesSent > maxMsgSize) {
            // if bundle is too large send all messages separately
            Log.Warn(this, "bundled msg too large, size=" + bytesSent + " maxMsgSize=" + maxMsgSize + ", #msg=" + (objects.size()-1));
            bytesSent = 0;
            int curMaxMsgSize = 0;
            // last object is sequence number!
            for (int j = 0; j < objects.size() - 1; j++) {
                int curSeqNr = (j == 0) ? seqNr : sendSequence.incrementAndGet();
                objArr = new Object[]{objects.get(j), curSeqNr};
                asByteArray = conf.asByteArray(objArr);
                int curLength = asByteArray.length;
                curMaxMsgSize = Math.max(curMaxMsgSize, curLength);
                bytesSent += curLength;
                if (curLength > maxMsgSize) {
                    Log.Error(this, "single msg too large, size=" + curLength + ", msg=" + objects.get(j));
                    // send msg anyway, can't hide random messages...
                }
                sendBinary(asByteArray);
            }
            if (curMaxMsgSize > maxMsgSizeSeen) {
                maxMsgSizeSeen = curMaxMsgSize;
                Log.Info(this, "new max single msg size seen: " + curMaxMsgSize);
            }
        } else {
            sendBinary(asByteArray);
        }
        objects.clear();
        if (stats != null) {
            stats.updateBytesSent(bytesSent);
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

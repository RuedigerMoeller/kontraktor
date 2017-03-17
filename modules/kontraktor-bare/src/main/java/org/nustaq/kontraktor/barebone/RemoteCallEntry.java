/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.kontraktor.barebone;

import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 05/06/15.
 *
 * Mirrors Kontraktor's RemoteCallEntry
 */
public class RemoteCallEntry implements Serializable {

    long receiverKey; // id of published actor in host, contains cbId in case of callbacks
    long futureKey; // id of future if any
    String method;
    Object args[];
    byte[] serializedArgs;
    int queue;

    public RemoteCallEntry(int receiverKey, int futureKey, String method, Object[] args, int queue) {
        this.receiverKey = receiverKey;
        this.futureKey = futureKey;
        this.method = method;
        this.args = args;
        this.queue = queue;
    }

    public long getReceiverKey() {
        return receiverKey;
    }

    public long getFutureKey() {
        return futureKey;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public int getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return "RemoteCallEntry{" +
                   "receiverKey=" + receiverKey +
                   ", futureKey=" + futureKey +
                   ", method='" + method + '\'' +
                   ", args=" + Arrays.toString(args) +
                   ", queue=" + queue +
                   '}';
    }

    public void pack(FSTConfiguration conf) {
        if ( args != null && serializedArgs == null ) {
            serializedArgs = conf.asByteArray(args);
            args = null;
        }
    }

    public void unpack(FSTConfiguration conf) {
        if ( args == null && serializedArgs != null ) {
            args = (Object[]) conf.asObject(serializedArgs);
            serializedArgs = null;
        }
    }

}

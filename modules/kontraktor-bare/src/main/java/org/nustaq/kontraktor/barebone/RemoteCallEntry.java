package org.nustaq.kontraktor.barebone;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 05/06/15.
 *
 * Mirrors Kontraktor's RemoteCallEntry
 */
public class RemoteCallEntry implements Serializable {

    int receiverKey; // id of published actor in host, contains cbId in case of callbacks
    int futureKey; // id of future if any
    String method;
    Object args[];
    int queue;

    public RemoteCallEntry(int receiverKey, int futureKey, String method, Object[] args, int queue) {
        this.receiverKey = receiverKey;
        this.futureKey = futureKey;
        this.method = method;
        this.args = args;
        this.queue = queue;
    }

    public int getReceiverKey() {
        return receiverKey;
    }

    public int getFutureKey() {
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
        return "BBRemoteCallEntry{" +
                   "receiverKey=" + receiverKey +
                   ", futureKey=" + futureKey +
                   ", method='" + method + '\'' +
                   ", args=" + Arrays.toString(args) +
                   ", queue=" + queue +
                   '}';
    }
}

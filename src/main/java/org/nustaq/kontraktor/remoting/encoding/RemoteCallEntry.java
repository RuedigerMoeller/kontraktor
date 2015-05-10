package org.nustaq.kontraktor.remoting.encoding;


import org.nustaq.kson.ArgTypes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 08.08.14.
 */
public class RemoteCallEntry implements Serializable {

    public static final int MAILBOX = 0;
    public static final int CBQ = 1;

    int receiverKey; // id of published actor in host, contains cbId in case of callbacks
    int futureKey; // id of future if any
    String method;
    @ArgTypes
    Object args[];
    int queue;

    public RemoteCallEntry(int futureKey, int receiverKey, String method, Object[] args) {
        this.receiverKey = receiverKey;
        this.futureKey = futureKey;
        this.method = method;
        this.args = args;
    }

    public int getQueue() {
        return queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public int getReceiverKey() {
        return receiverKey;
    }

    public void setReceiverKey(int receiverKey) {
        this.receiverKey = receiverKey;
    }

    public int getFutureKey() {
        return futureKey;
    }

    public void setFutureKey(int futureKey) {
        this.futureKey = futureKey;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
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
}


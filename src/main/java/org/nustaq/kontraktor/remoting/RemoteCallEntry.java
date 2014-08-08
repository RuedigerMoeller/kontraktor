package org.nustaq.kontraktor.remoting;

import java.io.Serializable;

/**
 * Created by ruedi on 08.08.14.
 */
public class RemoteCallEntry implements Serializable {

    public static final int MAILBOX = 0;
    public static final int CBQ = 1;

    int receiverKey;
    int senderKey;
    String method;
    Object args[];
    int queue;

    public RemoteCallEntry(int senderKey, int receiverKey, String method, Object[] args) {
        this.receiverKey = receiverKey;
        this.senderKey = senderKey;
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

    public int getSenderKey() {
        return senderKey;
    }

    public void setSenderKey(int senderKey) {
        this.senderKey = senderKey;
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
}

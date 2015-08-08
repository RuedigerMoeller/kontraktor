package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.ChangeMessage;
import org.nustaq.reallive.interfaces.Record;

/**
 * Created by ruedi on 07/08/15.
 */
public class PutMessage<K,V extends Record<K>> implements ChangeMessage<K,V> {

    public PutMessage(V record) {
        this.record = record;
    }

    private V record;

    public V getRecord() {
        return record;
    }

    @Override
    public int getType() {
        return PUT;
    }

    @Override
    public K getKey() {
        return record.getKey();
    }

    @Override
    public String toString() {
        return "PutMessage{" +
                ", record=" + record.asString() +
                '}';
    }

}

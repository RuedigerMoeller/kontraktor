package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.ChangeMessage;
import org.nustaq.reallive.interfaces.Record;

/**
 * Created by ruedi on 07/08/15.
 */
public class PutMessage<K> implements ChangeMessage<K> {

    public PutMessage(Record<K> record) {
        this.record = record;
    }

    private Record<K> record;

    public Record<K> getRecord() {
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
    public ChangeMessage reduced(String[] reducedFields) {
        return new PutMessage<>(record.reduced(reducedFields));
    }

    @Override
    public String toString() {
        return "PutMessage{" +
                ", record=" + record.asString() +
                '}';
    }

}

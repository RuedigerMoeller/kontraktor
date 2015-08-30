package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class AddMessage<K> implements ChangeMessage<K> {

    boolean updateIfExisting = false;
    private Record<K> record;

    public AddMessage(Record<K> record) {
        this.record = record;
    }

    public AddMessage(boolean updateIfExisting, Record<K> record) {
        this.updateIfExisting = updateIfExisting;
        this.record = record;
    }

    public Record<K> getRecord() {
        return record;
    }

    public boolean isUpdateIfExisting() {
        return updateIfExisting;
    }

    @Override
    public int getType() {
        return ADD;
    }

    @Override
    public K getKey() {
        return record.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new AddMessage<K>(updateIfExisting,record.reduced(reducedFields));
    }

    @Override
    public String toString() {
        return "AddMessage{" +
                "updateIfExisting=" + updateIfExisting +
                ", record=" + record.asString() +
                '}';
    }
}

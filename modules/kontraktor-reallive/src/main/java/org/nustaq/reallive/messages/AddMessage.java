package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class AddMessage implements ChangeMessage {

    boolean updateIfExisting = false;
    private Record record;

    public AddMessage(Record record) {
        this.record = record;
    }

    public AddMessage(boolean updateIfExisting, Record record) {
        this.updateIfExisting = updateIfExisting;
        this.record = record;
    }

    public Record getRecord() {
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
    public String getKey() {
        return record.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new AddMessage(updateIfExisting,record.reduced(reducedFields));
    }

    @Override
    public String toString() {
        return "AddMessage{" +
                "updateIfExisting=" + updateIfExisting +
                ", record=" + record.asString() +
                '}';
    }
}

package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class AddMessage implements ChangeMessage {

    boolean updateIfExisting = false;
    private Record record;
    int senderId;

    public AddMessage(int senderId, Record record) {
        this.record = record;
        this.senderId = senderId;
    }

    public AddMessage(int senderId, boolean updateIfExisting, Record record) {
        this.updateIfExisting = updateIfExisting;
        this.record = record;
        this.senderId = senderId;
    }

    public AddMessage senderId(int id) {
        senderId = id;
        return this;
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
    public int getSenderId() {
        return senderId;
    }

    @Override
    public String getKey() {
        return record.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new AddMessage(senderId, updateIfExisting,record.reduced(reducedFields));
    }

    @Override
    public String toString() {
        return "AddMessage{" +
                "updateIfExisting=" + updateIfExisting +
                ", record=" + record.asString() +
                '}';
    }
}

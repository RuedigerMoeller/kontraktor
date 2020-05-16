package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;
import org.nustaq.reallive.api.Record;

/**
 * Created by moelrue on 03.08.2015.
 */
public class RemoveMessage implements ChangeMessage {
    Record deletedRow;
    int senderId;

    public RemoveMessage(int senderId, Record rec) {
        this.deletedRow = rec;
        this.senderId = senderId;
    }
    
    public RemoveMessage senderId(int id) {
        senderId = id;
        return this;
    }

    @Override
    public int getType() {
        return REMOVE;
    }

    @Override
    public int getSenderId() {
        return senderId;
    }

    @Override
    public String getKey() {
        return deletedRow.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new RemoveMessage(senderId,deletedRow.reduced(reducedFields));
    }

    @Override
    public String toString() {
        return "RemoveMessage{" +
                "record=" + deletedRow.asString() +
                '}';
    }

    public Record getRecord() {
        return deletedRow;
    }
}

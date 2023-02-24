package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.ChangeMessage;
import org.nustaq.reallive.api.Record;


/**
 * Created by ruedi on 07/08/15.
 */
public class PutMessage implements ChangeMessage {

    int senderId;

    public PutMessage(int senderId, Record record) {
        this.record = record;
        this.senderId = senderId;
    }

    private Record record;

    public PutMessage senderId(int id) {
        senderId = id;
        return this;
    }

    public Record getRecord() {
        return record;
    }

    @Override
    public int getType() {
        return PUT;
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
        return new PutMessage(senderId,record.reduced(reducedFields));
    }

    @Override
    public ChangeMessage omit(String[] fields) {
        return new PutMessage(senderId,record.omit(fields));
    }

    @Override
    public String toString() {
        return "PutMessage{" +
            ", record=" + record.asString() +
            '}';
    }

}

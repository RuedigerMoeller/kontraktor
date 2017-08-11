package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class RemoveMessage implements ChangeMessage {
    Record deletedRow;

    public RemoveMessage(Record rec) {
        this.deletedRow = rec;
    }

    @Override
    public int getType() {
        return REMOVE;
    }

    @Override
    public String getKey() {
        return deletedRow.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new RemoveMessage(deletedRow.reduced(reducedFields));
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

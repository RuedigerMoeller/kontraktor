package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class RemoveMessage<K> implements ChangeMessage<K,Record> {
    Record<K> deletedRow;

    public RemoveMessage(Record<K> rec) {
        this.deletedRow = rec;
    }

    @Override
    public int getType() {
        return REMOVE;
    }

    @Override
    public K getKey() {
        return deletedRow.getKey();
    }

    @Override
    public String toString() {
        return "RemoveMessage{" +
                "record=" + deletedRow.asString() +
                '}';
    }

    public Record<K> getRecord() {
        return deletedRow;
    }
}

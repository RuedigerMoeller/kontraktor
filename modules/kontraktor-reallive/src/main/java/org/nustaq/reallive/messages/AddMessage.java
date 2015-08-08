package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public class AddMessage<K,V extends Record<K>> implements ChangeMessage<K,V> {

    boolean updateIfExisting = false;

    public AddMessage(V record) {
        this.record = record;
    }

    public AddMessage(boolean updateIfExisting, V record) {
        this.updateIfExisting = updateIfExisting;
        this.record = record;
    }

    private V record;

    public V getRecord() {
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
    public String toString() {
        return "AddMessage{" +
                "updateIfExisting=" + updateIfExisting +
                ", record=" + record.asString() +
                '}';
    }
}

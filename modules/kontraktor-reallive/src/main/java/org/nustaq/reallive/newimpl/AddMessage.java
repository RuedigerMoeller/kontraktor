package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public class AddMessage<K,V extends Record<K>> implements ChangeMessage<K,V> {

    public AddMessage(V record) {
        this.record = record;
    }

    private V record;

    public V record() {
        return record;
    }

}

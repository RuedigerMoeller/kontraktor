package org.nustaq.reallive.impl;

import org.nustaq.reallive.interfaces.ChangeReceiver;
import org.nustaq.reallive.interfaces.Mutation;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.messages.RemoveMessage;

/**
 * Created by ruedi on 08.08.2015.
 */
public class Mutator<K,V extends Record<K>> implements Mutation<K,V> {
    ChangeReceiver<K,V> receiver;

    public Mutator(ChangeReceiver<K, V> receiver) {
        this.receiver = receiver;
    }

    @Override
    public void put(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().put(key, keyVals));
    }

    @Override
    public void addOrUpdate(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    @Override
    public void add(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().add(key, keyVals));
    }

    @Override
    public void update(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public void remove(K key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        receiver.receive(remove);
    }
}

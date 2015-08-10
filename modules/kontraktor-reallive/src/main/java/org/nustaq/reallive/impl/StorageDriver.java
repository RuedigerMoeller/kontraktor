package org.nustaq.reallive.impl;

import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.*;

/**
 * Created by moelrue on 03.08.2015.
 *
 * implements transaction processing on top of a physical storage
 *
 */
public class StorageDriver<K,V extends Record<K>> implements ChangeReceiver<K,V>, Mutation<K,V> {

    RecordStorage<K,V> store;
    ChangeReceiver listener = change -> {};

    public StorageDriver(RecordStorage<K, V> store) {
        this.store = store;
    }

    public StorageDriver() {
    }

    @Override
    public void receive(ChangeMessage<K, V> change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.ADD:
            {
                AddMessage<K,V> addMessage = (AddMessage) change;
                V prevRecord = store.get(addMessage.getKey());
                if ( prevRecord != null && ! addMessage.isUpdateIfExisting() ) {
                    return;
                }
                if ( prevRecord != null ) {
                    Diff diff = ChangeUtils.copyAndDiff(addMessage.getRecord(), prevRecord);
                    V newRecord = prevRecord; // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage<>(diff,newRecord) );
                } else {
                    store.put(change.getKey(),addMessage.getRecord());
                    listener.receive(addMessage);
                }
                break;
            }
            case ChangeMessage.REMOVE:
            {
                RemoveMessage<K> removeMessage = (RemoveMessage) change;
                V v = store.remove(removeMessage.getKey());
                if ( v != null ) {
                    listener.receive(new RemoveMessage<>(v));
                }
                break;
            }
            case ChangeMessage.UPDATE:
            {
                UpdateMessage<K,V> updateMessage = (UpdateMessage<K, V>) change;
                V oldRec = store.get(updateMessage.getKey());
                if ( oldRec == null && updateMessage.isAddIfNotExists() ) {
                    if ( updateMessage.getNewRecord() == null ) {
                        throw new RuntimeException("updated record does not exist, cannot fall back to 'Add' as UpdateMessage.newRecord is null");
                    }
                    store.put(change.getKey(),updateMessage.getNewRecord());
                    listener.receive( new AddMessage(updateMessage.getNewRecord()) );
                } else if ( updateMessage.getDiff() == null ) {
                    Diff diff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec);
                    V newRecord = oldRec; // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage<>(diff,newRecord) );
                } else {
                    // old values are actually not needed inside the diff
                    // however they are needed in a change notification for filter processing (need to reconstruct prev record)
                    Diff newDiff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec, updateMessage.getDiff().getChangedFields());
                    V newRecord = oldRec; // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage(newDiff,newRecord));
                }
            }
        }
    }

    public RecordStorage<K, V> getStore() {
        return store;
    }

    public ChangeReceiver getListener() {
        return listener;
    }

    public StorageDriver store(final RecordStorage<K, V> store) {
        this.store = store;
        return this;
    }

    public StorageDriver setListener(final ChangeReceiver listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void put(K key, Object... keyVals) {
        receive(RLUtil.get().put(key,keyVals));
    }

    @Override
    public void addOrUpdate(K key, Object... keyVals) {
        receive(RLUtil.get().addOrUpdate(key,keyVals));
    }

    @Override
    public void add(K key, Object... keyVals) {
        receive(RLUtil.get().add(key,keyVals));
    }

    @Override
    public void add(Record<K> rec) {
        receive((ChangeMessage<K, V>) new AddMessage<K,Record<K>>(rec));
    }

    @Override
    public void update(K key, Object... keyVals) {
        receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public void remove(K key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        receive(remove);
    }
}
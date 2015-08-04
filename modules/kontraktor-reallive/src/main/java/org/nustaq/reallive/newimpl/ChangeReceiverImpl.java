package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public class ChangeReceiverImpl<K,V extends Record<K>> implements ChangeReceiver<K,V> {

    RecordStore<K,V> store;
    ChangeReceiver listener;

    public ChangeReceiverImpl(RecordStore<K, V> store) {
        this.store = store;
    }

    public ChangeReceiverImpl() {
    }

    @Override
    public void receive(ChangeMessage<K, V> change) {
        switch (change.getType()) {
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
                    listener.receive( new UpdateMessage<>(diff,newRecord) );
                } else {
                    // old values are actually not needed inside the diff
                    // however they are needed in a change notification for filter processing (need to reconstruct prev record)
                    Diff newDiff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec, updateMessage.getDiff().getChangedFields());
                    V newRecord = oldRec; // clarification
                    listener.receive( new UpdateMessage(newDiff,newRecord));
                }
            }
        }
    }

    public RecordStore<K, V> getStore() {
        return store;
    }

    public ChangeReceiver getListener() {
        return listener;
    }

    public ChangeReceiverImpl store(final RecordStore<K, V> store) {
        this.store = store;
        return this;
    }

    public ChangeReceiverImpl listener(final ChangeReceiver listener) {
        this.listener = listener;
        return this;
    }

}

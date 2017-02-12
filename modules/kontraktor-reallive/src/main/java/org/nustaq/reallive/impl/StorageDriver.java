package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.PatchingRecord;
import org.nustaq.reallive.records.RecordWrapper;

/**
 * Created by moelrue on 03.08.2015.
 *
 * implements transaction processing on top of a physical storage
 *
 */
public class StorageDriver<K> implements ChangeReceiver<K>, Mutation<K> {

    RecordStorage<K> store;
    ChangeReceiver listener = change -> {};

    public StorageDriver(RecordStorage<K> store) {
        this.store = store;
        Log.Info(this,""+store.getStats());
    }

    public StorageDriver() {
    }

    public static Record unwrap(Record r) {
        if ( r instanceof PatchingRecord ) {
            return unwrap(((PatchingRecord) r).unwrapOrCopy());
        }
        if ( r instanceof RecordWrapper ) {
            return unwrap(((RecordWrapper) r).getRecord());
        }
        return r;
    }

    @Override
    public void receive(ChangeMessage<K> change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.PUT:
            {
                Record<K> prevRecord = store.get(change.getKey());
                if ( prevRecord == null ) {
                    store.put(change.getKey(),unwrap(change.getRecord()));
                    receive( new AddMessage<K>(true,change.getRecord()));
                } else {
                    Diff diff = ChangeUtils.diff(change.getRecord(), prevRecord);
                    Record<K> newRecord = unwrap(change.getRecord()); // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage<>(diff,newRecord,null) );
                }
                break;
            }
            case ChangeMessage.ADD:
            {
                AddMessage<K> addMessage = (AddMessage) change;
                K key = addMessage.getKey();
                Record<K> prevRecord = store.get(key);
                if ( prevRecord != null && ! addMessage.isUpdateIfExisting() ) {
                    return;
                }
                if ( prevRecord != null ) {
                    Diff diff = ChangeUtils.copyAndDiff(addMessage.getRecord(), prevRecord);
                    Record<K> newRecord = unwrap(prevRecord); // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage<>(diff,newRecord,null) );
                } else {
                    store.put(change.getKey(),unwrap(addMessage.getRecord()));
                    listener.receive(addMessage);
                }
                break;
            }
            case ChangeMessage.REMOVE:
            {
                RemoveMessage<K> removeMessage = (RemoveMessage) change;
                Record<K> v = store.remove(removeMessage.getKey());
                if ( v != null ) {
                    listener.receive(new RemoveMessage<>(unwrap(v)));
                } else {
//                    System.out.println("*********** failed remove "+change.getKey());
//                    store.put(change.getKey(), new MapRecord<K>(change.getKey()).put("url", "POK"));
//                    System.out.println("  reput and get:" + store.get(change.getKey()));
//                    store.remove(change.getKey());
//                    System.out.println("  re-rem and get:" + store.get(change.getKey()));
//                    store.filter( rec -> rec.getKey().equals(change.getKey()), (r,e) -> {
//                        System.out.println("  "+r);
//                    });
                }
                break;
            }
            case ChangeMessage.UPDATE:
            {
                UpdateMessage<K> updateMessage = (UpdateMessage<K>) change;
                Record<K> oldRec = store.get(updateMessage.getKey());
                if ( oldRec == null && updateMessage.isAddIfNotExists() ) {
                    if ( updateMessage.getNewRecord() == null ) {
                        throw new RuntimeException("updated record does not exist, cannot fall back to 'Add' as UpdateMessage.newRecord is null");
                    }
                    store.put(change.getKey(),updateMessage.getNewRecord());
                    listener.receive( new AddMessage(updateMessage.getNewRecord()) );
                } else if ( updateMessage.getDiff() == null ) {
                    Diff diff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec);
                    Record<K> newRecord = unwrap(oldRec); // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage<>(diff,newRecord,change.getForcedUpdateFields()) );
                } else {
                    // old values are actually not needed inside the diff
                    // however they are needed in a change notification for filter processing (need to reconstruct prev record)
                    Diff newDiff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec, updateMessage.getDiff().getChangedFields());
                    Record<K> newRecord = unwrap(oldRec); // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage(newDiff,newRecord,change.getForcedUpdateFields()));
                }
                break;
            }
            default:
                throw new RuntimeException("unknown change type "+change.getType());
        }
    }

    public RecordStorage<K> getStore() {
        return store;
    }

    public ChangeReceiver getListener() {
        return listener;
    }

    public StorageDriver store(final RecordStorage<K> store) {
        this.store = store;
        return this;
    }

    public StorageDriver setListener(final ChangeReceiver listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object... keyVals) {
        Record<K> kRecord = getStore().get(key);
        if ( casCondition == null || casCondition.test(kRecord) ) {
            put(key,keyVals);
            return new Promise(true);
        }
        return new Promise(false);
    }

    @Override
    public void put(K key, Object ... keyVals) {
        receive(RLUtil.get().put(key,keyVals));
    }

    @Override
    public void atomic(K key, RLConsumer action) {
        Record<K> rec = getStore().get(key);
        if ( rec == null ) {
            action.accept(rec);
        } else {
            PatchingRecord pr = new PatchingRecord(rec);
            action.accept(pr);
            UpdateMessage updates = pr.getUpdates();
            if ( updates != null ) {
                receive(updates);
            }
        }
    }

    /**
     * apply the function to the record with given key and return the result inside a promise
     *
     * changes to the record inside the function are applied to the real record and a change message
     * is generated.
     *
     * In case the function returns a changemessage (add,put,remove ..), the change message is applied
     * to the original record and the change is broadcasted
     *
     * @param key
     * @param action
     * @return the result of function.
     */
    @Override
    public IPromise atomicQuery(K key, RLFunction<Record<K>,Object> action) {
        Record<K> rec = getStore().get(key);
        if ( rec == null ) {
            final Object apply = action.apply(rec);
            if ( apply instanceof ChangeMessage )
            {
                receive( (ChangeMessage) apply ) ;
            }
            return new Promise(apply);
        } else {
            PatchingRecord pr = new PatchingRecord(rec);
            final Object res = action.apply(pr);
            if ( res instanceof ChangeMessage )
            {
                receive( (ChangeMessage) res ) ;
            } else {
                UpdateMessage updates = pr.getUpdates();
                if (updates != null) {
                    receive(updates);
                }
            }
            return new Promise<>(res);
        }
    }

    @Override
    public void atomicUpdate(RLPredicate<Record<K>> filter, RLFunction<Record<K>, Boolean> action) {
        store.filter(filter, (r,e) -> {
            if ( r != null ) {
                PatchingRecord pr = new PatchingRecord(r);
                Boolean res = action.apply(pr);
                if (res==Boolean.FALSE) {
                    receive(RLUtil.get().remove((K) pr.getKey()));
                } else {
                    UpdateMessage updates = pr.getUpdates();
                    if (updates != null) {
                        receive(updates);
                    }
                }
            }
        });
    }

    @Override
    public void addOrUpdate(K key, Object... keyVals) {
        receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    @Override
    public void add(K key, Object... keyVals) {
        receive(RLUtil.get().add(key, keyVals));
    }

    @Override
    public void add(Record<K> rec) {
        receive(new AddMessage<K>(rec));
    }

    @Override
    public void addOrUpdateRec(Record<K> rec) {
        receive(new AddMessage<K>(true,rec));
    }

    @Override
    public void put(Record<K> rec) {
        receive( new PutMessage<K>(rec) );
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

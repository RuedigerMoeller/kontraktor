package org.nustaq.reallive.server;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.PatchingRecord;
import org.nustaq.reallive.records.RecordWrapper;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;

/**
 * Created by moelrue on 03.08.2015.
 *
 * implements transaction processing on top of a physical storage
 *
 */
public class StorageDriver implements ChangeReceiver {

    public static boolean PROPAGATE_EMPTY_DIFFS = true;

    RecordStorage store;
    ChangeReceiver listener = change -> {};

    public StorageDriver(RecordStorage store) {
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
    public void receive(ChangeMessage change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.PUT:
            {
                Record prevRecord = store.get(change.getKey());
                if ( prevRecord == null ) {
                    store.put(change.getKey(),unwrap(change.getRecord()));
                    receive( new AddMessage(change.getSenderId(),true,change.getRecord()));
                } else {
                    Diff diff = ChangeUtils.diff(change.getRecord(), prevRecord);
                    if ( ! diff.isEmpty() || PROPAGATE_EMPTY_DIFFS ) {
                        Record newRecord = unwrap(change.getRecord()); // clarification
                        store.put(change.getKey(), newRecord);
                        listener.receive(new UpdateMessage(change.getSenderId(), diff, newRecord, null));
                    }
                }
                break;
            }
            case ChangeMessage.ADD:
            {
                AddMessage addMessage = (AddMessage) change;
                String key = addMessage.getKey();
                Record prevRecord = store.get(key);
                if ( prevRecord != null && ! addMessage.isUpdateIfExisting() ) {
                    return;
                }
                if ( prevRecord != null ) {
                    Diff diff = ChangeUtils.copyAndDiff(addMessage.getRecord(), prevRecord);
                    Record newRecord = unwrap(prevRecord); // clarification
                    store.put(change.getKey(),newRecord);
                    listener.receive( new UpdateMessage(change.getSenderId(), diff,newRecord,null) );
                } else {
                    store.put(change.getKey(),unwrap(addMessage.getRecord()));
                    listener.receive(addMessage);
                }
                break;
            }
            case ChangeMessage.REMOVE:
            {
                RemoveMessage removeMessage = (RemoveMessage) change;
                Record v = store.remove(removeMessage.getKey());
                if ( v != null ) {
                    listener.receive(new RemoveMessage(change.getSenderId(),unwrap(v)));
                } else {
//                    System.out.println("*********** failed remove "+change.getKey());
//                    store.putRecord(change.getKey(), new MapRecord<K>(change.getKey()).putRecord("url", "POK"));
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
                UpdateMessage updateMessage = (UpdateMessage) change;
                Record oldRec = store.get(updateMessage.getKey());
                if ( oldRec == null && updateMessage.isAddIfNotExists() ) {
                    if ( updateMessage.getNewRecord() == null ) {
                        throw new RuntimeException("updated record does not exist, cannot fall back to 'Add' as UpdateMessage.newRecord is null");
                    }
                    store.put(change.getKey(),updateMessage.getNewRecord());
                    listener.receive( new AddMessage(change.getSenderId(), updateMessage.getNewRecord()) );
                } else if ( updateMessage.getDiff() == null ) {
                    Diff diff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec);
                    if ( ! diff.isEmpty() || PROPAGATE_EMPTY_DIFFS ) {
                        Record newRecord = unwrap(oldRec); // clarification
                        store.put(change.getKey(), newRecord);
                        listener.receive(new UpdateMessage(change.getSenderId(), diff, newRecord, change.getForcedUpdateFields()));
                    }
                } else {
                    // old values are actually not needed inside the diff
                    // however they are needed in a change notification for filter processing (need to reconstruct prev record)
                    if ( ! updateMessage.getDiff().isEmpty() || PROPAGATE_EMPTY_DIFFS ) {
                        Diff newDiff = ChangeUtils.copyAndDiff(updateMessage.getNewRecord(), oldRec, updateMessage.getDiff().getChangedFields());
                        Record newRecord = unwrap(oldRec); // clarification
                        store.put(change.getKey(), newRecord);
                        listener.receive(new UpdateMessage(change.getSenderId(), newDiff, newRecord, change.getForcedUpdateFields()));
                    }
                }
                break;
            }
            default:
                throw new RuntimeException("unknown change type "+change.getType());
        }
    }

    public RecordStorage getStore() {
        return store;
    }

    public ChangeReceiver getListener() {
        return listener;
    }

    public StorageDriver store(final RecordStorage store) {
        this.store = store;
        return this;
    }

    public StorageDriver setListener(final ChangeReceiver listener) {
        this.listener = listener;
        return this;
    }

    public void resizeIfLoadFactorLarger( double loadFactor, long maxGrowBytes ) {
        store.resizeIfLoadFactorLarger(loadFactor, maxGrowBytes);
    }

    public void put(int senderId, String key, Object ... keyVals) {
        receive(RLUtil.get().put(senderId, key,keyVals));
    }

    /**
     * apply the function to the record with given key and return the result inside a promise
     *
     * changes to the record inside the function are applied to the real record and a change message
     * is generated.
     *
     * In case the function returns a changemessage (add,putRecord,remove ..), the change message is applied
     * to the original record and the change is broadcasted
     *
     * @param key
     * @param action
     * @return the result of function.
     */
    public IPromise atomicQuery(int senderId, String key, RLFunction<Record,Object> action) {
        Record rec = getStore().get(key);
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
                ((ChangeMessage) res).senderId(senderId);
                receive( (ChangeMessage) res ) ;
            } else {
                UpdateMessage updates = pr.getUpdates(senderId);
                if (updates != null) {
                    receive(updates);
                }
            }
            return new Promise(res);
        }
    }

    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        store.forEach(filter, (r,e) -> {
            if ( r != null ) {
                PatchingRecord pr = new PatchingRecord(r);
                Boolean res = action.apply(pr);
                if (res==Boolean.FALSE) {
                    receive(RLUtil.get().remove(0,pr.getKey()));
                } else {
                    UpdateMessage updates = pr.getUpdates(0);
                    if (updates != null) {
                        receive(updates);
                    }
                }
            }
        });
    }

    public void addOrUpdate(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().addOrUpdate(senderId,key, keyVals));
    }

    public void add(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().add(senderId,key, keyVals));
    }

    public void add(int senderId, Record rec) {
        receive(new AddMessage(senderId,rec));
    }

    public void addOrUpdateRec(int senderId, Record rec) {
        receive(new AddMessage(senderId,true,rec));
    }

    public void put(int senderId, Record rec) {
        receive( new PutMessage(senderId,rec) );
    }

    public void update(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().update(senderId,key, keyVals));
    }

    public void remove(int senderId, String key) {
        RemoveMessage remove = RLUtil.get().remove(senderId,key);
        receive(remove);
    }

    public void _saveMapping(ClusterTableRecordMapping mapping) {
        store._saveMapping(mapping);
    }

    public ClusterTableRecordMapping _loadMapping() {
        return store._loadMapping();
    }

}

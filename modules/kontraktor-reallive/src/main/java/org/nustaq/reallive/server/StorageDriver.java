package org.nustaq.reallive.server;

import org.nustaq.kontraktor.Callback;
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
 */
public class StorageDriver implements ChangeReceiver {

    public static boolean PROPAGATE_EMPTY_DIFFS = true;

    RecordStorage store;
    ChangeReceiver listener = change -> {};

    public StorageDriver(RecordStorage store) {
        this.store = store;
        Log.Info(this,""+store.getStats());
    }

    public static Record unwrap(Record r) {
        if ( r instanceof PatchingRecord ) {
            return unwrap(((PatchingRecord) r).unwrapOrCopy());
        }
        if ( r instanceof RecordWrapper ) {
            return unwrap(r.getRecord());
        }
        return r;
    }

    /**
     * Semantics:
     * We expect the change has NOT YET been applied.
     * In case of updates we expect the diff to be correct. In case the diff is null, we compute it right here.
     * @param change
     */
    @Override
    public void receive(ChangeMessage change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.PUT:
            {
                Record prevRecord = store.get(change.getKey());
                if ( prevRecord == null ) {
                    if ( change.updateLastModified() )
                        store.put(change.getKey(),unwrap(change.getRecord()));
                    else
                        store._rawPut(change.getKey(),unwrap(change.getRecord()));
                    if ( change.generateChangeBroadcast() )
                        receive( new AddMessage(change.getSenderId(),true,change.getRecord()));
                } else {
                    Record newRecord = change.getRecord();
                    Diff diff = ChangeUtils.computeDiff(prevRecord,newRecord);
                    if ( !diff.isEmpty() || PROPAGATE_EMPTY_DIFFS ) {
                        if ( change.updateLastModified() )
                            store.put(change.getKey(), newRecord);
                        else
                            store._rawPut(change.getKey(), newRecord);
                        if ( change.generateChangeBroadcast() )
                            listener.receive(new UpdateMessage(change.getSenderId(), diff, newRecord));
                    }
                }
                break;
            }
            case ChangeMessage.REMOVE:
            {
                RemoveMessage removeMessage = (RemoveMessage) change;
                String key = removeMessage.getKey();
                Record v = store.remove(key);
                if ( v != null ) {
                    if ( change.generateChangeBroadcast() )
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
            case ChangeMessage.ADD:
            {
                AddMessage addMessage = (AddMessage) change;
                String key = addMessage.getKey();
                Record prevRecord = store.get(key);
                if ( prevRecord != null && ! addMessage.isUpdateIfExisting() ) {
                    return;
                }
                if ( prevRecord == null ) { // add
                    store.put(change.getKey(), addMessage.getRecord());
                    if ( change.generateChangeBroadcast() )
                        listener.receive(addMessage);
                    return;
                }
                // break; fall through to update
                change = new UpdateMessage(addMessage.getSenderId(),null,addMessage.getRecord());
            }
            case ChangeMessage.UPDATE:
            {
                UpdateMessage updateMessage = (UpdateMessage) change;
                Record previousRec = store.get(updateMessage.getKey());
                if ( previousRec == null && updateMessage.isAddIfNotExists() ) {
                    if ( updateMessage.getNewRecord() == null ) {
                        throw new RuntimeException("updated record does not exist, cannot fall back to 'Add' as UpdateMessage.newRecord is null");
                    }
                    store.put(change.getKey(),updateMessage.getNewRecord());
                    if ( change.generateChangeBroadcast() )
                        listener.receive( new AddMessage(change.getSenderId(), updateMessage.getNewRecord()) );
                } else {
                    Diff diff = updateMessage.getDiff();
                    Record updatedRecord = change.getRecord();
                    if ( diff == null ) {
                        diff = ChangeUtils.computeDiff(previousRec, updatedRecord);
                    }
                    // old values are actually not needed inside the diff
                    // however they are needed in a change notification for filter processing (need to reconstruct prev record)
                    if ( ! diff.isEmpty() || PROPAGATE_EMPTY_DIFFS ) {
                        diff.applyToOldRecord(previousRec,updatedRecord);
                        store.put(change.getKey(), previousRec);
                        if ( change.generateChangeBroadcast() )
                            listener.receive(new UpdateMessage(change.getSenderId(), diff, previousRec ));
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
    public IPromise atomic(int senderId, String key, RLFunction<Record,Object> action) {
        Record rec = getStore().get(key);
        if ( rec == null ) {
            final Object apply = action.apply(rec);
            if ( apply instanceof ChangeMessage )
            {
                receive( (ChangeMessage) apply ) ;
            }
            return new Promise(apply);
        } else {
            Record pr = rec.deepCopy();
            final Object res = action.apply(pr);
            if ( res instanceof ChangeMessage )
            {
                ((ChangeMessage) res).senderId(senderId);
                receive( (ChangeMessage) res ) ;
            } else {
                Diff diff = ChangeUtils.computeDiff(rec, pr);
                if ( !diff.isEmpty())
                    receive(new UpdateMessage(senderId,diff,pr));
            }
            return new Promise(res);
        }
    }

    public void atomicQuery(int senderId, RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        store.forEach(filter, (r,e) -> {
            if ( r != null ) {
                Record pr = r.deepCopy();
                Boolean res = action.apply(pr);
                if (res==Boolean.FALSE) {
                    receive(RLUtil.get().remove(senderId,pr.getKey()));
                } else {
                    Diff diff = ChangeUtils.computeDiff(r, pr);
                    if ( ! diff.isEmpty() )
                        receive(new UpdateMessage(senderId,diff,pr));
                }
            }
        });
    }

    public void add(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().add(senderId,key, keyVals));
    }

    public void add(int senderId, Record rec) {
        receive(new AddMessage(senderId,rec));
    }

    public void put(int senderId, Record rec) {
        receive( new PutMessage(senderId,rec) );
    }

    public void update(int senderId, String key, Object... keyVals) {
        Record currentRecord = getStore().get(key);
        if ( currentRecord == null ) {
            receive(RLUtil.get().add(senderId,key,keyVals));
        } else {
            Record modifiedRecord = currentRecord.deepCopy();
            for (int i = 0; i < keyVals.length; i+=2) {
                Object k = keyVals[i];
                Object v = keyVals[i+1];
                modifiedRecord.put((String)k,v);
            }
            receive(new UpdateMessage(senderId,null,modifiedRecord));
        }
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

    public void queryRemoveLog(long from, long to, Callback<RemoveLog.RemoveLogEntry> cb) {
        RemoveLog removeLog = store.getRemoveLog();
        if ( removeLog == null ) {
            cb.finish();
        } else {
            removeLog.query(from,to,cb);
        }
    }

    public void pruneRemoveLog( long maxAge ) {
        RemoveLog removeLog = store.getRemoveLog();
        if ( removeLog != null ) {
            removeLog.prune(maxAge);
        }
    }
}

package org.nustaq.kontraktor.services.rlserver;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.impl.storage.RecordJsonifier;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.UpdateMessage;
import org.nustaq.reallive.query.QParseException;
import org.nustaq.reallive.records.MapRecord;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RLJsonSession<T extends RLJsonSession> extends Actor<T> implements RemotedActor {

    public static int senderIdRangeStart=100_000, senderIdRangeEnd = 5_000_000; // WARNING: needs organization for multiple instances

    protected static AtomicInteger senderIdCount;

    protected RLJsonServer server;
    protected DataClient dClient;
    protected int senderId;

    public void init( RLJsonServer server, DataClient dataClient, Object userdata ) {
        synchronized (RLJsonSession.class ) {
            if ( senderIdCount == null )
                senderIdCount = new AtomicInteger(senderIdRangeStart);
            senderId = senderIdCount.getAndIncrement();
            if ( senderId >= senderIdRangeEnd ) {
                senderIdCount.set(senderIdRangeStart);
            }
        }
        this.server = server;
        this.dClient = dataClient;
    }

    public IPromise<Integer> getSenderId() {
        return resolve(senderId);
    }

    public IPromise update(String table, String json ) {
        try {
            updateAsync(table, json);
        } catch (Exception e) {
            String message = e.getMessage();
            return reject(message==null ? e.toString() : e );
        }
        return resolve(true);
    }

    public void updateAsync(String table, String json) {
        JsonValue parse = Json.parse(json);
        if ( ! parse.isObject() ) {
            throw new RuntimeException("not a json object:"+json);
        }
        JsonObject members = parse.asObject();
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            throw new RuntimeException("table '"+table+"' not found");
        Record record = toRecord(members);
        if ( record.getKey() == null )
            throw new RuntimeException("no key in record");
        _internalUpdate(tbl,record);
    }

    public IPromise delete(String table, String key ) {
        try {
            deleteAsync(table, key);
        } catch (Exception e) {
            String message = e.getMessage();
            return reject(message==null ? e.toString() : e );
        }
        return resolve(true);
    }

    public IPromise<Set<String>> fieldsOf( String table ) {
        Promise res = new Promise();
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            return reject("table '"+table+"' not found");
        SchemaSpore.apply(tbl).then( (r,e) -> {
            if ( r != null )
                res.resolve(r.toArray(new String[r.size()]));
            else
                res.reject(e);
        });
        return res;
    }

    public IPromise<String> get( String table, String key ) {
        IPromise res = new Promise();
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            reject("table '"+table+"' not found");
        tbl.get(key).then( (r,e) -> {
            if ( r != null ) {
                res.resolve(fromRecord(r).toString());
            } else
                res.reject(e);
        });
        return res;
    }

    // test
    public void selectHashed(String table, String indexPath2hashKeyJson, String query, Callback<String> res) {
        RealLiveTable tbl = dClient.tbl(table);
        AtomicBoolean hadErr = new AtomicBoolean(false);
        if ( tbl == null )
            res.reject("table '"+table+"' not found");
        JsonObject parse;
        try {
            parse = Json.parse(indexPath2hashKeyJson).asObject();
        } catch (Exception e) {
            Log.Info(this,e);
            res.reject(e);
            return;
        }
        RLHashIndexPredicate predicate = new RLHashIndexPredicate(new QueryPredicate(query));
        parse.names().forEach( name -> {
            if ( name.startsWith("-") ) {
                predicate.subtract(name.substring(1),RecordJsonifier.get().toJavaValue(parse.get(name)));
            } else if ( name.startsWith("/") ) {
                predicate.intersect(name.substring(1),RecordJsonifier.get().toJavaValue(parse.get(name)));
            } else
                predicate.join(name,RecordJsonifier.get().toJavaValue(parse.get(name)));
        });
        tbl.forEach( predicate, (r, e) -> {
            if ( r != null )
                res.pipe(fromRecord(r).toString());
            else if ( e != null ) {
                if (!hadErr.get()) {
                    if (e instanceof QParseException) {
                        res.reject("Error in Query:" + ((QParseException) e).getMessage());
                    } else
                        res.reject(e);
                    hadErr.set(true);
                } else {
                    // do nothing
                }
            }
            else
                res.finish();
        });
    }

    public void select(String table, String query, Callback<String> res) {
        RealLiveTable tbl = dClient.tbl(table);
        AtomicBoolean hadErr = new AtomicBoolean(false);
        if ( tbl == null )
            res.reject("table '"+table+"' not found");
        tbl.query(query, (r,e) -> {
            if ( r != null )
                res.pipe(fromRecord(r).toString());
            else if ( e != null ) {
                if (!hadErr.get()) {
                    if (e instanceof QParseException) {
                        res.reject("Error in Query:" + ((QParseException) e).getMessage());
                    } else
                        res.reject(e);
                    hadErr.set(true);
                } else {
                    // do nothing
                }
            }
            else
                res.finish();
        });
    }

    protected static class SubsEntry {
        Callback feCB;
        Subscriber subs;

        public SubsEntry(Callback feCB, Subscriber subs) {
            this.feCB = feCB;
            this.subs = subs;
        }
    }

    protected Map<String,SubsEntry> subscriptions = new HashMap<>();
    public void unsubscribe( String uuid ) {
        SubsEntry subsEntry = subscriptions.get(uuid);
        if ( subsEntry != null ) {
            Callback callback = subsEntry.feCB;
            dClient.unsubscribe(subsEntry.subs.getId());
            if (callback != null) {
                callback.finish();
                subscriptions.remove(uuid);
            }
        }
    }

    public void subscribe(String uuid, String table, String query, Callback<String> res) {
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            res.reject("table '"+table+"' not found");
        Subscriber subscriber = tbl.subscribeOn(query, (change) -> {
            if (change != null)
                res.pipe(fromChange(change).toString());
            else
                res.finish();
        });
        subscriptions.put(uuid, new SubsEntry(res,subscriber));
    }

    public IPromise<Long> subscribeSyncing(String uuid, String table, long timeStamp, String query, Callback<String> res) {
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            res.reject("table '"+table+"' not found");
        QueryPredicate filter = new QueryPredicate(query);
        Subscriber subs = new Subscriber( rec -> rec.getLastModified() >= timeStamp && filter.test(rec),(change) -> {
            if ( change != null )
                res.pipe(fromChange(change).toString());
            else
                res.finish();
        });
        subscriptions.put(uuid,new SubsEntry(res,subs));
        tbl.subscribe(subs);
        return resolve(System.currentTimeMillis());
    }

    protected JsonObject fromChange( ChangeMessage change ) {
        switch ( change.getType() ) {
            case ChangeMessage.ADD: {
                JsonObject result = Json.object();
                result.set("type", "ADD");
                result.set("senderId", change.getSenderId());
                result.set("record", fromRecord(change.getRecord()));
                return result;
            }
            case ChangeMessage.REMOVE:
            {
                JsonObject result = Json.object();
                result.set("type", "REMOVE");
                result.set("senderId", change.getSenderId());
                result.set("record", fromRecord(change.getRecord()));
                return result;
            }
            case ChangeMessage.UPDATE: {
                JsonObject result = Json.object();
                result.set("type", "UPDATE");
                result.set("senderId", change.getSenderId());
                result.set("record", fromRecord(change.getRecord()));
                JsonObject diff = new JsonObject();
                UpdateMessage upd = (UpdateMessage) change;
                String[] changedFields = upd.getDiff().getChangedFields();
                Object[] oldValues = upd.getDiff().getOldValues();
                for (int i = 0; i < changedFields.length; i++) {
                    String changedField = changedFields[i];
                    diff.set(changedField, RecordJsonifier.get().fromJavaValue(oldValues[i]) );
                }
                result.set("diff", diff);
                return result;
            }
            case ChangeMessage.QUERYDONE:
                JsonObject result = Json.object();
                result.set("type","QUERYDONE");
                return result;
            default:
                Log.Error(this,"unexpected change type");
        }
        return null;
    }

    public void deleteAsync(String table, String key ) {
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            throw new RuntimeException("table '"+table+"' not found");
        tbl.remove(senderId,key);
    }

    /**
     * process a series of add/update operations. Input is a map<recordkey,list<updateObjects>.
     * Atomic Array Ops can be done like
     * { "array+" : value } - add value to existing array
     * { "array-" : value } - remove all equal values from existing array
     * { "array?+" : value } - add only if value does not yet exist (set-like behaviour)
     * _NULL_ - can be used to denote null values (real null will be evicted by json )
     *
     * @param table
     * @param json - [ addOrUpdate, .. ]
     * @return
     */
    public IPromise<Long> bulkUpdate(String table, String json ) {
        try {
            JsonObject parse = Json.parse(json).asObject();
            RealLiveTable tbl = dClient.tbl(table);
            parse.forEach( member -> {
                member.getValue().asArray().forEach( addupd -> {
                    try {
                        JsonObject obj = addupd.asObject();
                        Record newRecord = toRecord(obj);
                        newRecord.key(member.getName());
                        _internalUpdate(tbl, newRecord);
                        // avoid getting stuck
                    } catch (Exception e) {
                        Log.Error(this,e);
                    }
                });
            });
        } catch ( Exception e ) {
            return reject(e);
        }
        return resolve(System.currentTimeMillis());
    }

    /**
     * processes special ops like { "array+" : value } and _NULL_
     * @param tbl
     * @param newRecord
     */
    protected void _internalUpdate(RealLiveTable tbl, Record newRecord) {
        int finalSID = this.senderId;
        tbl.atomic(finalSID,newRecord.getKey(), currentRecord -> {
            if ( currentRecord != null ) {
                String[] fields = newRecord.getFields();
                for (int i = 0; i < fields.length; i++) {
                    String field = fields[i];
                    if ( field.endsWith("+") ) // atomic array insert
                    {
                        boolean set = field.endsWith("?+");
                        Object toAdd = newRecord.get(field);
                        String pureField = field.substring(0,field.length()- (set ? 2 : 1));
                        Object o = currentRecord.get(pureField);
                        if ( o instanceof Object[] ) {
                            Object[] oldarr = (Object[]) o;
                            boolean matched = false;
                            if ( set ) {
                                for (int j = 0; j < oldarr.length; j++) {
                                    Object o1 = oldarr[j];
                                    if ( Objects.deepEquals(o1,toAdd) ) {
                                        matched = true;
                                        break;
                                    }
                                }
                            }
                            if ( ! matched ) {
                                Object newCopy[] = new Object[oldarr.length + 1];
                                System.arraycopy(oldarr, 0, newCopy, 0, oldarr.length);
                                newCopy[oldarr.length] = toAdd;
                                currentRecord.put(pureField, newCopy);
                            }
                        } else {
                            currentRecord.put( pureField, new Object[] { toAdd } );
                        }
                    } else if ( field.endsWith("-") ) // atomic array remove
                    {
                        String purefield = field.substring(0,field.length()-1);
                        Object toRem = newRecord.get(field);
                        if ("_NULL_".equals(toRem) )
                            toRem = null;
                        Object o = currentRecord.get(purefield);
                        Object[] oldarr = (Object[]) o;
                        Object finalToRem = toRem;
                        Object[] collect = Arrays.asList(oldarr).stream().filter(x -> !Objects.deepEquals(x, finalToRem)).collect(Collectors.toList()).toArray();
                        currentRecord.put(purefield,collect);
                    } else {
                        currentRecord.put(field,newRecord.get(field));
                    }
                }
                return null;
            }
            Object[] keyVals = newRecord.getKeyVals();
            for (int i = 0; i < keyVals.length; i+=2) {
                String keyVal = (String) keyVals[i];
                if ( keyVal.endsWith("?+") )
                    keyVals[i] = keyVal.substring(0,keyVal.length()-2);
                else if ( keyVal.endsWith("-") )
                    keyVals[i] = keyVal.substring(0,keyVal.length()-1);
                else if ( keyVal.endsWith("+") )
                    keyVals[i] = keyVal.substring(0,keyVal.length()-1);
            }
            return RLUtil.get().addOrUpdate(finalSID,newRecord.getKey(),keyVals);
        });
    }

    protected JsonObject fromRecord(Record r) {
        return RecordJsonifier.get().fromRecord(r);
    }

    protected Record toRecord(JsonObject members) {
        return RecordJsonifier.get().toRecord(members);
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {
        Map<String,SubsEntry> copy = new HashMap<>(subscriptions.size());
        copy.putAll(subscriptions);
        copy.forEach( (k,en) -> unsubscribe(k) );
    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {

    }

}

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
import org.nustaq.reallive.api.ChangeMessage;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.Subscriber;
import org.nustaq.reallive.impl.QueryPredicate;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.UpdateMessage;
import org.nustaq.reallive.query.QParseException;
import org.nustaq.reallive.records.MapRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RLJsonSession extends Actor<RLJsonSession> implements RemotedActor {

    public static int senderIdRangeStart=100_000, senderIdRangeEnd = 500_000; // WARNING: needs organization for multiple instances

    static AtomicInteger senderIdCount;

    private RLJsonServer server;
    private DataClient dClient;
    int senderId;

    public void init(RLJsonServer server, DataClient dataClient ) {
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
        tbl.mergeRecord(senderId,record);
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

    static class SubsEntry {
        Callback feCB;
        Subscriber subs;

        public SubsEntry(Callback feCB, Subscriber subs) {
            this.feCB = feCB;
            this.subs = subs;
        }
    }

    Map<String,SubsEntry> subscriptions = new HashMap<>();
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
        AtomicBoolean hadErr = new AtomicBoolean(false);
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
        AtomicBoolean hadErr = new AtomicBoolean(false);
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
                    diff.set(changedField, fromJavaValue(oldValues[i]) );
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
     * @param table
     * @param json - [ addOrUpdate, .. ]
     * @return
     */
    public IPromise<Boolean> bulkUpdate(String table, String json ) {
        try {
            JsonArray parse = Json.parse(json).asArray();
            RealLiveTable tbl = dClient.tbl(table);
            parse.forEach( addupd -> {
                JsonObject obj = addupd.asObject();
                tbl.mergeRecord(senderId, toRecord(obj) );
            });
        } catch ( Exception e ) {
            return reject(e);
        }
        return resolve(true);
    }

    JsonObject fromRecord(Record r) {
        String[] fields = r.getFields();
        String key = r.getKey();
        JsonObject res = new JsonObject();
        if ( key != null )
            res.set("key",key);
        res.set("lastModified",r.getLastModified());
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object value = r.get(field);
            res.set(field,fromJavaValue(value));
        }
        return res;
    }

    private JsonValue fromJavaValue(Object value) {
        if ( value instanceof String ) {
            return Json.value((String)value);
        } else if ( value instanceof Long ) {
            return Json.value(((Number) value).longValue());
        } else if ( value instanceof Integer ) {
            return Json.value(((Number) value).intValue());
        } else if ( value instanceof Number ) {
            return Json.value(((Number) value).doubleValue());
        } else if ( value instanceof Boolean ) {
            return Json.value( ((Boolean) value).booleanValue() );
        } else if ( value instanceof Object[] ) {
            JsonArray jarr = new JsonArray();
            Object arr[] = (Object[]) value;
            for (int j = 0; j < arr.length; j++) {
                jarr.add(fromJavaValue(arr[j]));
            }
            return jarr;
        } else if ( value instanceof Record ) {
            return fromRecord((Record) value);
        } else if ( value == null ) {
            return Json.NULL;
        } else {
            System.out.println("unmapped data "+value);
        }
        return Json.value(""+value);
    }

    Record toRecord(JsonObject members) {
        MapRecord aNew = MapRecord.New(null);
        members.names().forEach( field -> {
            if ( "key".equals(field) ) {
                aNew.key(members.get(field).asString());
                return;
            }
            JsonValue jsonValue = members.get(field);
            if ( jsonValue.isString() ) {
                aNew.put(field,jsonValue.asString());
            } else if ( jsonValue.isNull() ) {
                aNew.put(field,null);
            } else if ( jsonValue.isNumber() ) {
                if ( jsonValue.toString().indexOf('.') >= 0 )
                    aNew.put(field,jsonValue.asDouble());
                else
                    aNew.put(field,jsonValue.asLong());
            } else if ( jsonValue.isBoolean() ) {
                aNew.put(field,jsonValue.asBoolean());
            } else if ( jsonValue.isObject() ) {
                aNew.put(field,toRecord(jsonValue.asObject()));
            } else if ( jsonValue.isArray() ) {
                aNew.put(field,toRecordArray(jsonValue.asArray()));
            } else {
                throw new RuntimeException("huch:"+jsonValue.getClass());
            }
        });
        return aNew;
    }

    Object[] toRecordArray(JsonArray arr) {
        Object res[] = new Object[arr.size()];
        int i = 0;
        for (JsonValue jsonValue : arr) {
            if ( jsonValue.isString() ) {
                res[i] = jsonValue.asString();
            } else if ( jsonValue.isNull() ) {
                res[i] = null;
            } else if ( jsonValue.isNumber() ) {
                res[i] = jsonValue.asDouble();
            } else if ( jsonValue.isBoolean() ) {
                res[i] = jsonValue.asBoolean();
            } else if ( jsonValue.isObject() ) {
                res[i] = toRecord(jsonValue.asObject());
            } else if ( jsonValue.isArray() ) {
                res[i] = toRecordArray(jsonValue.asArray());
            } else {
                throw new RuntimeException("huch:"+jsonValue.getClass());
            }
            i++;
        }
        return res;
    }


    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {

    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {

    }

}

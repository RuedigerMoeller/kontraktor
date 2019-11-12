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
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.query.QParseException;
import org.nustaq.reallive.records.MapRecord;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RLJsonSession extends Actor<RLJsonSession> implements RemotedActor {

    private RLJsonServer server;
    private DataClient dClient;

    public void init(RLJsonServer server, DataClient dataClient ) {
        this.server = server;
        this.dClient = dataClient;
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
        tbl.mergeRecord(record);
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

    public void deleteAsync(String table, String key ) {
        RealLiveTable tbl = dClient.tbl(table);
        if ( tbl == null )
            throw new RuntimeException("table '"+table+"' not found");
        tbl.remove(key);
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
                aNew.key(members.get(field).toString());
                return;
            }
            JsonValue jsonValue = members.get(field);
            if ( jsonValue.isString() ) {
                aNew.put(field,jsonValue.asString());
            } else if ( jsonValue.isNull() ) {
                aNew.put(field,null);
            } else if ( jsonValue.isNumber() ) {
                aNew.put(field,jsonValue.asDouble());
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

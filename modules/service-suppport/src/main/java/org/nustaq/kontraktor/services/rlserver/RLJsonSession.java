package org.nustaq.kontraktor.services.rlserver;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.MapRecord;

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
        System.out.println("pok "+record);
        tbl.mergeRecord(record);
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

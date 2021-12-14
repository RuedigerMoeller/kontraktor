package org.nustaq.reallive.server.storage;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.MapRecord;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * converts json to records and vice versa
 *
 * used types:
 * String - string
 * Boolean - boolean
 * Long,Double - number
 * Object[] - array
 * Record - { }
 *
 */
public class RecordJsonifier {

    static RecordJsonifier singleton = new RecordJsonifier();

    public static RecordJsonifier get() {
        return singleton;
    }

    public JsonObject fromRecord(Record r) {
        String key = r.getKey();
        JsonObject res = new JsonObject();
        if ( key != null )
            res.set("key",key);
        res.set("lastModified",r.getLastModified());
        String[] fields = r.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object value = r.get(field);
            res.set(field,fromJavaValue(value));
        }
        return res;
    }

    public JsonValue fromJavaValue(Object value) {
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
        } else if ( value instanceof Collection ) {
            JsonArray jarr = new JsonArray();
            ((Collection<?>) value).forEach( v -> jarr.add(fromJavaValue(v)) );
            return jarr;
        } else if ( value instanceof Record ) {
            return fromRecord((Record) value);
        } else if ( value == null ) {
            return Json.NULL;
        } else if ( value instanceof JsonValue ) {
            return (JsonValue) value;
        } else if ( value instanceof Map ) {
            return from((Map)value).toJson();
        } else {
            if ( value != null )
                System.out.println("unmapped data "+value.getClass().getName() );
            System.out.println("unmapped data "+value);
        }
        return Json.value(""+value);
    }

    public Object toRecordValue(Object value) {
        if ( value instanceof Collection ) {
            return ((Collection<?>) value).toArray(new Object[((Collection<?>) value).size()]);
        } else if ( value instanceof Map ) {
            return from((Map)value);
        }
        return value;
    }

    public Record from( Map<String,Object> map ) {
        return Record.from(
            map.entrySet().stream()
                .flatMap(en -> Stream.of(en.getKey(), toRecordValue(en.getValue())))
                .collect(Collectors.toList())
                .toArray()
        );
    }

    public Record toRecord(JsonObject members) {
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
                throw new RuntimeException("unexpected json type:"+jsonValue.getClass());
            }
        });
        return aNew;
    }

    public Object toJavaValue(JsonValue jsonValue) {
        if ( jsonValue.isString() ) {
            return jsonValue.asString();
        } else if ( jsonValue.isNull() ) {
            return null;
        } else if ( jsonValue.isNumber() ) {
            if ( jsonValue.toString().indexOf('.') >= 0 )
                return jsonValue.asDouble();
            else
                return jsonValue.asLong();
        } else if ( jsonValue.isBoolean() ) {
            return jsonValue.asBoolean();
        } else {
            throw new RuntimeException("unexpected json type:"+jsonValue.getClass());
        }
    }

    public Object[] toRecordArray(JsonArray arr) {
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
                throw new RuntimeException("unexpected json type:"+jsonValue.getClass());
            }
            i++;
        }
        return res;
    }

    public static JsonObject jsonFrom(Object... keyVals) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < keyVals.length; i += 2) {
            String key = (String) keyVals[i];
            Object val = keyVals[i + 1];
            if (val instanceof String) obj.set(key, (String) val);
            else if (val == null) obj.set(key, Json.NULL);
            else if (val instanceof Integer) obj.set(key, (int) val);
            else if (val instanceof Double) obj.set(key, (double) val);
            else if (val instanceof Long) obj.set(key, (long) val);
            else if (val instanceof Float) obj.set(key, (float) val);
            else if (val instanceof Boolean) obj.set(key, (boolean) val);
            else if (val instanceof JsonObject) obj.set(key, (JsonObject) val);
            else if (val instanceof Object[]) {
                Object[] arr = (Object[]) val;
                JsonArray jarr = jsonArrayFrom(arr);
                obj.set(key, jarr);
            } else throw new RuntimeException("unexpected type " + val);
        }
        return obj;
    }

    public static JsonArray jsonArrayFrom(Object ... arr) {
        JsonArray jarr = new JsonArray();
        for (Object o : arr) {
            if (o instanceof String) jarr.add((String) o);
            else if (o instanceof Integer) jarr.add((int) o);
            else if (o instanceof Double) jarr.add((double) o);
            else if (o instanceof Long) jarr.add((long) o);
            else if (o instanceof Float) jarr.add((float) o);
            else if (o instanceof Boolean) jarr.add((boolean) o);
            else if (o instanceof Object[]) jarr.add(jsonArrayFrom((Object[]) o));
            else if (o instanceof JsonObject) jarr.add((JsonObject) o);
            else throw new RuntimeException("unexpected type " + o);
        }
        return jarr;
    }

    public static void main(String[] args) {
        JsonObject json = jsonFrom( "test", 13, "otherTest", "Hello");
        String key = "9991";
        Record targetRec = Record.from("appointmentProviderClass", Record.from());
        System.out.println(targetRec.toPrettyString());
        System.out.println();
        Record rec = Record.from(
            "key", key,
            "availabilityRules" + "+",
            Record.from("appointmentProviderClass" + "+", //not working
                Record.from("rec",
                    Record.from(json))));
        System.out.println("merging:");
        System.out.println(rec.toPrettyString());
        System.out.println();
        targetRec.deepMerge(
            rec
        );
        System.out.println("result:");
        System.out.println(targetRec.toPrettyString());
        System.out.println();

//        Record r = Record.from("x", Map.of("1", "val1", "2", Map.of("3", "val3")));
//        System.out.println(r.toPrettyString());
//        Record rr = Record.from(r.toJson());
//        System.out.println(r);
//        System.out.println(rr);
    }

}

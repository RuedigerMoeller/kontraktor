package org.nustaq.kontraktor.services.rlserver.mongodb;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.MapRecord;

import java.util.ArrayList;
import java.util.List;

public class MongoUtil {

    static MongoUtil singleton = new MongoUtil();

    public static MongoUtil get() {
        return singleton;
    }

    public Document fromRecord(Record r) {
        String key = r.getKey();
        Document res = new Document();
        if ( key != null )
            res.put("key",key);
        res.put("lastModified",r.getLastModified());
        String[] fields = r.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if ( "_id".equals(field) )
                continue;
            Object value = r.get(field);
            res.put(field,fromJavaValue(value));
        }
        return res;
    }

    public Object fromJavaValue(Object value) {
//        if ( value instanceof String ) {
//            return Json.value((String)value);
//        } else if ( value instanceof Long ) {
//            return Json.value(((Number) value).longValue());
//        } else if ( value instanceof Integer ) {
//            return Json.value(((Number) value).intValue());
//        } else if ( value instanceof Number ) {
//            return Json.value(((Number) value).doubleValue());
//        } else if ( value instanceof Boolean ) {
//            return Json.value( ((Boolean) value).booleanValue() );
//        } else
        if ( value instanceof Object[] ) {
            ArrayList jarr = new ArrayList(((Object[]) value).length);
            Object arr[] = (Object[]) value;
            for (int j = 0; j < arr.length; j++) {
                jarr.add(fromJavaValue(arr[j]));
            }
            return jarr;
        } else if ( value instanceof Record ) {
            return fromRecord((Record) value);
        } else if ( value == null ) {
            return null;//Json.NULL;
        } else {
            //System.err.println("unmapped data "+value);
        }
        return value;
    }

    public Record toRecord(Document doc) {
        MapRecord aNew = MapRecord.New(null);
        doc.entrySet().forEach( entry -> {
            String field = entry.getKey();
            if ( "key".equals(field) ) {
                aNew.key(doc.get(field).toString());
                return;
            } else if ( "_id".equals(field) ) {
                aNew.put("_id", ((ObjectId)doc.get(field)).toString());
                return;
            }
            Object jsonValue = doc.get(field);
            if ( jsonValue instanceof String ) {
                aNew.put(field,jsonValue);
            } else if ( jsonValue == null ) {
                aNew.put(field,null);
            } else if ( jsonValue instanceof Byte ) {
                aNew.put(field, ((Byte) jsonValue).longValue());
            } else if ( jsonValue instanceof Short ) {
                aNew.put(field, ((Short) jsonValue).shortValue());
            } else if ( jsonValue instanceof Integer ) {
                aNew.put(field, ((Integer) jsonValue).intValue());
            } else if ( jsonValue instanceof Long ) {
                aNew.put(field, ((Long) jsonValue).longValue());
            } else if ( jsonValue instanceof Float ) {
                aNew.put(field, ((Float) jsonValue).floatValue());
            } else if ( jsonValue instanceof Double ) {
                aNew.put(field, ((Double) jsonValue).doubleValue());
            } else if ( jsonValue instanceof Boolean ) {
                aNew.put(field,((Boolean) jsonValue).booleanValue());
            } else if ( jsonValue instanceof Document ) {
                aNew.put(field,toRecord((Document) jsonValue));
            } else if ( jsonValue instanceof List) {
                aNew.put(field,toRecordArray((List) jsonValue));
            } else {
                throw new RuntimeException("unexpected json type:"+jsonValue.getClass());
            }
        });
        return aNew;
    }

    public Object[] toRecordArray(List arr) {
        Object res[] = new Object[arr.size()];
        int i = 0;
        for (Object jsonValue : arr) {
            if ( jsonValue instanceof String || jsonValue instanceof Number || jsonValue instanceof Boolean) {
                res[i] = jsonValue;
            } else if ( jsonValue instanceof Document ) {
                res[i] = toRecord((Document) jsonValue);
            } else if ( jsonValue instanceof List ) {
                res[i] = toRecordArray((List) jsonValue);
            } else {
                throw new RuntimeException("unexpected json type:"+jsonValue.getClass());
            }
            i++;
        }
        return res;
    }

}

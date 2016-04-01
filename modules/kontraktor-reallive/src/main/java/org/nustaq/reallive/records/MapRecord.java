package org.nustaq.reallive.records;

import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.RLUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by ruedi on 04.08.2015.
 */
public class MapRecord<K> implements Record<K> {

    public static Class<? extends MapRecord> recordClass = MapRecord.class;
    public static Function<MapRecord,MapRecord> conversion;

    public static <K> MapRecord<K> New() {
        try {
            return recordClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Map<String,Object> map = new HashMap<>();

    protected String fields[];
    protected K key;

    protected MapRecord() {
    }

    public static <K> MapRecord<K> New(K key) {
        MapRecord mapRecord = New();
        mapRecord.key = key;
        return mapRecord;
    }

    public static <K> MapRecord<K> New(K key, Object ... values) {
        MapRecord mapRecord = New();
        RLUtil.get().buildRecord(mapRecord,values);
        return mapRecord;
    }

    public int size() {
        return map.size();
    }

    @Override
    public K getKey() {
        return key;
    }

//    @Override
    public void key(K key) {
        this.key = key;
    }

    @Override
    public String[] getFields() {
        if (fields==null) {
            fields = new String[map.size()];
            map.keySet().toArray(fields);
        }
        return fields;
    }

    @Override
    public Object get(String field) {
        return map.get(field);
    }

    @Override
    public MapRecord put(String field, Object value) {
        field=field.intern();
        if ( map.put(field, value) == null ) {
            fields = null;
        }
        if (value == null)
            map.remove(field);
        return this;
    }

    @Override
    public String toString() {
        return "MapRecord{" + asString() + '}';
    }

    /**
     * @return a shallow copy
     */
    public MapRecord<K> copied() {
        MapRecord<K> newReq = MapRecord.New(getKey());
        map.forEach( (k,v) -> newReq.put(k,v) );
        return newReq;
    }
}

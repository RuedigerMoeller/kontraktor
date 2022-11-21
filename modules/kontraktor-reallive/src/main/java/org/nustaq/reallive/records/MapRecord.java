package org.nustaq.reallive.records;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.nustaq.reallive.server.RLUtil;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 04.08.2015.
 *
 * a record stored by reallive.
 *
 */
public class MapRecord implements Record {

    public static Function<Object,Boolean> JDK_TYPE_CHECK = value -> {
        Class<?> clazz = value.getClass();
        String name = clazz.getName();
        if ( isSimpleType(name) ||
            (clazz.isArray() &&
                (clazz.getComponentType().isPrimitive() ||
                    isSimpleType(clazz.getComponentType().getName()) ) ) )
        {
            return true;
        } else {
            return false;
        }
    };

    public static Function<Object,Boolean> JSON_CHECKER = value -> {
        if (
            value == null ||
            value instanceof String ||
            value instanceof Number ||
            value instanceof Boolean ||
            value instanceof Record
        )
        {
            return true;
        } else {
            if ( value instanceof Object[] )
            {
                return checkTypesIn((Object[]) value);
            }
            return false;
        }
    };

    private static Boolean checkTypesIn(Object[] value) {
        Object[] arr = value;
        for (int i = 0; i < arr.length; i++) {
            Object o = arr[i];
            if ( o == null ||
                o instanceof String ||
                o instanceof Number ||
                o instanceof Boolean ||
                o instanceof Record
            ) {
                continue;
            } else {
               if ( o instanceof Object[] && checkTypesIn((Object[]) o) )
                   continue;
               return false;
            }
        }
        return true;
    }

    static Function<Object,Boolean> CHECK_TYPES = JSON_CHECKER;

    public static Class<? extends MapRecord> recordClass = MapRecord.class;
    public static Function<MapRecord,MapRecord> conversion;

    static MapRecord New() {
        try {
            return recordClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Map<String,Object> map = new Object2ObjectOpenHashMap(3,0.75f);

    protected transient String fields[];
    protected String key;
    protected long lastModified, seq;

    protected MapRecord() {
    }

    public static MapRecord New(String key) {
        MapRecord mapRecord = New();
        mapRecord.key = key;
        return mapRecord;
    }

    public static MapRecord New(String key, Object ... values) {
        MapRecord mapRecord = New(key);
        RLUtil.get().buildRecord(mapRecord,values);
        return mapRecord;
    }

    public int size() {
        return map.size();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void internal_setLastModified(long tim) {
        lastModified = tim;
    }

    @Override
    public void internal_incSequence() {
        seq++; // requires updating is single threaded !
    }

    @Override
    public long getSequence() {
        return seq;
    }

    @Override
    public Record internal_put(String key, Object value) {
        map.put(key,value);
        return this;
    }

    @Override
    public Record key(String key) {
        this.key = key;return this;
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
    public MapRecord put( String key, Object value ) {
        if ( map.put(key, value) == null ) { // delete attribute
            fields = null;
        }
        if (value == null || _NULL_.equals(value) )
            map.remove(key);
        else if ( CHECK_TYPES != null ) {
            if ( CHECK_TYPES.apply(value) ) {
                map.put(key,value);
            } else {
                throw new RuntimeException("tried to store non-allowed value types key:"+key+" val:"+((value!=null) ? value.getClass().getName() : "null") );
            }
        } else
            map.put(key,value);
        return this;
    }

    private static boolean isSimpleType(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") ||
            name.equals(MapRecord.class.getName());
    }

    @Override
    public String toString() {
        return "MapRecord{" + asString() + '}';
    }

    /**
     * @return a shallow copy
     */
    public MapRecord shallowCopy() {
        MapRecord newReq = MapRecord.New(getKey());
        map.forEach( (k,v) -> newReq.put(k,v) );
        newReq.internal_setLastModified(lastModified);
        newReq.internal_setSeq(seq);
        return newReq;
    }

    public MapRecord deepCopied() {
        MapRecord newReq = MapRecord.New(getKey());
        map.forEach( (k,v) -> newReq.put(k,mapValue(v)) );
        newReq.internal_setLastModified(lastModified);
        newReq.internal_setSeq(seq);
        return newReq;
    }

    private static Object mapValue(Object v) {
        if ( v instanceof Record )
            v = ((Record) v).deepCopied();
        else if ( v instanceof Object[] ) {
            Object[] varr = (Object[]) v;
            Object arr[] = Arrays.copyOf(varr, varr.length );
            for (int j = 0; j < arr.length; j++) {
                arr[j] = mapValue(arr[j]);
            }
            return arr;
        }
        return v;
    }

    private void internal_setSeq(long seq) {
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        return defaultEquals(o);
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public Set<String> getFieldSet() {
        return map.keySet();
    }

    public static void main(String[] args) {
        MapRecord rec = MapRecord.New("pok");
        rec.put("x", new Object[] { 1,2,3});

    }

    @Override
    public boolean _afterLoad() {
        if ( map instanceof HashMap ) {
            Map<String, Object> newMap = new Object2ObjectOpenHashMap<>(map.size(),0.75f);
            map.forEach( (k,v) -> {
                newMap.put( k, v );
                if ( v instanceof Record )
                    ((Record) v)._afterLoad();
            });
            map = newMap;
            return true;
        }
        return false;
    }
}

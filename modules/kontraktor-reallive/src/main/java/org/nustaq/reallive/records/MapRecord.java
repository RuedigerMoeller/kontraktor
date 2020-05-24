package org.nustaq.reallive.records;

import org.nustaq.reallive.server.RLUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 04.08.2015.
 *
 * a record stored by reallive.
 *
 */
public class MapRecord implements Record {

    public static boolean CHECK_TYPES = true;
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

    protected Map<String,Object> map = new HashMap();

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
        if (value == null)
            map.remove(key);
        else if ( CHECK_TYPES ){
            Class<?> clazz = value.getClass();
            String name = clazz.getName();
            if ( isSimpleType(name) ||
                (clazz.isArray() &&
                    (clazz.getComponentType().isPrimitive() ||
                        isSimpleType(clazz.getComponentType().getName()) ) ) )
            {
                map.put(key,value);
            } else {
                throw new RuntimeException("allowed values: jdk classes and instanceof Record");
            }
        } else
            map.put(key,value);
        return this;
    }

    private boolean isSimpleType(String name) {
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
    public MapRecord copied() {
        MapRecord newReq = MapRecord.New(getKey());
        map.forEach( (k,v) -> newReq.put(k,v) );
        newReq.internal_setLastModified(lastModified);
        newReq.internal_setSeq(seq);
        return newReq;
    }

    private void internal_setSeq(long seq) {
        this.seq = seq;
    }
}

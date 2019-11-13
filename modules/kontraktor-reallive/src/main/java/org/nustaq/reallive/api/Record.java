package org.nustaq.reallive.api;

import org.nustaq.reallive.query.EvalContext;
import org.nustaq.reallive.query.LongValue;
import org.nustaq.reallive.query.StringValue;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.records.MapRecord;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record extends Serializable, EvalContext {

    String getKey();
    long getLastModified();
    void internal_setLastModified(long tim);
    void internal_incSequence();
    long getSequence(); // increments with each update of the record

    default void internal_updateLastModified() {
        internal_setLastModified(System.currentTimeMillis());
        internal_incSequence();
    }

    /**
     * take care, kind of dangerous
     * @param key
     */
    Record key(String key);
    String[] getFields();
    Record put( String field, Object value );

    @Override
    default Value getValue(String field) {
        if ( "_key".equals(field))
            return new StringValue( getKey(), null );
        if ( "_lastModified".equals(field))
            return new LongValue( getLastModified(), null );
        return EvalContext.super.getValue(field);
    }

    default int getInt(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).intValue();
    }

    default long getLong(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).longValue();
    }

    default Record getRec(String field) {
        Object val = get(field);
        if ( val == null )
            return MapRecord.New(null);
        return (Record) val;
    }

    /**
     * creates and sets an empty record in case
     * @param field
     * @return
     */
    default Record rec(String field) {
        Object val = get(field);
        if ( val == null ) {
            MapRecord aNew = MapRecord.New(null);
            put(field,aNew);
            return aNew;
        }
        return (Record) val;
    }

    /**
     * creates and sets an empty list in case
     * @param field
     * @return
     */
    default List arr(String field) {
        Object val = get(field);
        if ( val == null ) {
            ArrayList aNew = new ArrayList();
            put(field,aNew);
            return aNew;
        }
        return (List) val;
    }

    /**
     * creates and sets an empty list in case
     * @param field
     * @return
     */
    default List<Record> recarr(String field) {
        Object val = get(field);
        if ( val == null ) {
            ArrayList aNew = new ArrayList();
            put(field,aNew);
            return aNew;
        }
        return (List) val;
    }

    default double getDouble(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).doubleValue();
    }

    default String getString(String field) {
        Object val = get(field);
        if ( val == null )
            return null;
        return val.toString();
    }

    default String getSafeString(String field) {
        Object val = get(field);
        if ( val == null )
            return "";
        return val.toString();
    }

    default String asString() {
        String[] fields = getFields();
        String res = "[  *"+getKey()+"  ";
        for (int i = 0; i < fields.length; i++) {
            String s = fields[i];
            res += s+"="+get(s)+", ";
        }
        return res+"]";
    }

    default boolean getBool(String field) {
        Object val = get(field);
        if ( val instanceof Boolean == false )
            return false;
        return ((Boolean) val).booleanValue();
    }

    default Record reduced(String[] reducedFields) {
        MapRecord rec = MapRecord.New(getKey());
        for (int i = 0; i < reducedFields.length; i++) {
            String reducedField = reducedFields[i];
            Object val = get(reducedField);
            if ( val != null ) {
                rec.put(reducedField,val);
            }
        }
        return rec;
    }

    default Record copied() {
        throw new RuntimeException("copy not implemented");
    }

    default Object[] getKeyVals() {
        final String[] fields = getFields();
        Object[] res = new Object[fields.length*2];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res[i*2] = field;
            res[i*2+1] = get(field);
        }
        return res;
    }

    default Map<String,Object> asMap() {
        HashMap<String,Object> res = new HashMap<>();
        final String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res.put(field,getValue(field));
        }
        return res;
    }

    /**
     * copy all fields from given record to this
     * @param record
     */
    default void merge(Record record) {
        final String[] fields = record.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            put( field, record.get(field) );
        }
    }

}

package org.nustaq.reallive.interfaces;

import org.nustaq.reallive.query.EvalContext;
import org.nustaq.reallive.query.StringValue;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.records.MapRecord;

import java.io.Serializable;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record<K> extends Serializable, EvalContext {

    K getKey();

    /**
     * take care, kind of dangerous
     * @param key
     */
    void key(K key);
    String[] getFields();
    Record put( String field, Object value );

    @Override
    default Value getValue(String field) {
        if ( "_key".equals(field))
            return new StringValue( getKey().toString() );
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

    default Record<K> reduced(String[] reducedFields) {
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

    default Record<K> copied() {
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
}

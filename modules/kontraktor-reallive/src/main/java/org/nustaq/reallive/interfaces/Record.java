package org.nustaq.reallive.interfaces;

import org.nustaq.reallive.query.EvalContext;

import java.io.Serializable;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record<K> extends Serializable, EvalContext {

    K getKey();
    String[] getFields();
    Object get( String field );
    Record put( String field, Object value );

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
        if ( val == null )
            return false;
        return true;
    }
}

package org.nustaq.kontraktor.weblication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by ruedi on 18.07.17.
 */
public final class PersistedRecord implements Serializable {
    Map mp;
    String key;

    public PersistedRecord(String key) {
        this.mp = new HashMap(7);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    Object get(String key) {
        return mp.get(key);
    }

    public int getInt(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).intValue();
    }

    public void forEach(BiConsumer<String,Object> iterator) {
        mp.forEach(iterator);
    }

    public long getLong(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).longValue();
    }

    public double getDouble(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).doubleValue();
    }

    public String getString(String field) {
        Object val = get(field);
        if ( val == null )
            return null;
        return val.toString();
    }

    public boolean getBool(String field) {
        Object val = get(field);
        if ( val instanceof Boolean == false )
            return false;
        return ((Boolean) val).booleanValue();
    }

    public void put( String key, Object value ) {
        if ( value != null ) {
            Class<?> clazz = value.getClass();
            String name = clazz.getName();
            if ( isSimpleType(name) ||
                (clazz.isArray() &&
                    (clazz.getComponentType().isPrimitive() ||
                     isSimpleType(clazz.getComponentType().getName()) ) ) )
            {
                mp.put(key,value);
            } else {
                throw new RuntimeException("allowed values: jdk classes and instanceof PersistedRecord");
            }
        } else {
            mp.put(key,value);
        }
    }

    private boolean isSimpleType(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") ||
                    name.equals(PersistedRecord.class.getName());
    }

    public static void main(String[] args) {
        System.out.println( Integer.class.getName() );
        System.out.println( int[].class.getName() );
    }
}

package org.nustaq.reallive.api;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record<K> {

    K getKey();
    String[] getFields();
    Object get( String field );
    Record put( String field, Object value );

    default String asString() {
        String[] fields = getFields();
        String res = "[  *"+getKey()+"  ";
        for (int i = 0; i < fields.length; i++) {
            String s = fields[i];
            res += s+"="+get(s)+", ";
        }
        return res+"]";
    }
}

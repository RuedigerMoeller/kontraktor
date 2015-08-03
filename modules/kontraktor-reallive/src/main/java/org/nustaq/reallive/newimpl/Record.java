package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record<K> {

    K getKey();
    String[] getFields();
    Object get( String field );
    Record put( String field, Object value );

}

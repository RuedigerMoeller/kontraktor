package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface RecordStore<T extends Record> extends ChangeReceiver<T> {

    public T get( Object key );

}

package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeMessage<K,V> {

    int ADD = 0;
    int REMOVE = 1;
    int UPDATE = 2;

    int getType();

    K getKey();

}

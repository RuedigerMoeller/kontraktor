package org.nustaq.reallive.interfaces;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeMessage<K> {

    int ADD = 0;
    int REMOVE = 1;
    int UPDATE = 2;
    int QUERYDONE = 3;
    int PUT = 4;

    int getType();

    K getKey();

}

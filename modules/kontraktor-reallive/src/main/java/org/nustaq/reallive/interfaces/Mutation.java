package org.nustaq.reallive.interfaces;

/**
 * Created by moelrue on 05.08.2015.
 */
public interface Mutation<K,V> {

    void put(K key, Object... keyVals);
    void addOrUpdate(K key, Object... keyVals);
    void add( K key, Object ... keyVals );
    void update( K key, Object ... keyVals );
    void remove(K key);

}

package org.nustaq.reallive.actors;

/**
 * Created by moelrue on 06.08.2015.
 */
public interface ShardFunc<K> {
    int apply(K key);
}

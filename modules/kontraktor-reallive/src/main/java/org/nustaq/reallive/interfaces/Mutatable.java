package org.nustaq.reallive.interfaces;

/**
 * Created by ruedi on 08.08.2015.
 */
public interface Mutatable<K,V extends Record<K>> {

    Mutation<K,V> getMutation();

}

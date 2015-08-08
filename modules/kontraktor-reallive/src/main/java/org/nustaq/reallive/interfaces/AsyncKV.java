package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 07/08/15.
 */
public interface AsyncKV<K,V> {

    IPromise<V> get( K key );
    IPromise<Long> size();

}

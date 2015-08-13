package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 07/08/15.
 */
public interface AsyncKV<K> {

    IPromise<Record<K>> get( K key );
    IPromise<Long> size();

}

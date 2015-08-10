package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable<K, V extends Record<K>> extends ChangeReceiver<K,V>, RecordIterable<K,V>, ChangeStream<K,V>, AsyncKV<K,V>, Mutatable<K,V> {

    IPromise ping();
    IPromise<TableDescription> getDescription();

}

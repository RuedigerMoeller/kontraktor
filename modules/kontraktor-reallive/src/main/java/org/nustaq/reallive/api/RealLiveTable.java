package org.nustaq.reallive.api;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable<K, V extends Record<K>> extends ChangeReceiver<K,V>, Mutation<K,V>, RecordIterable<K,V>, ChangeStream<K,V>{

    IPromise ping();

}

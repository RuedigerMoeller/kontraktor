package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 06/08/15.
 */
public interface RealLiveTable<K> extends ChangeReceiver<K>, RecordIterable<K>, ChangeStream<K>, AsyncKV<K>, Mutatable<K> {

    IPromise ping();
    IPromise<TableDescription> getDescription();
    void stop();
}

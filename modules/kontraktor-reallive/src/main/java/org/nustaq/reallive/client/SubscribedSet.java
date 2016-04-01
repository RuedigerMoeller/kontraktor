package org.nustaq.reallive.client;

import org.nustaq.reallive.impl.StorageDriver;
import org.nustaq.reallive.impl.storage.HeapRecordStorage;
import org.nustaq.reallive.interfaces.*;

/**
 * Created by ruedi on 22/08/15.
 *
 * Threadsafe for sharded tables in a non-actor usage scenario
 */
public class SubscribedSet<K> {

    final RealLiveTable<K> source;
    StorageDriver<K> storage;
    Subscriber<K> subs;

    public SubscribedSet(RealLiveTable<K> source) {
        this.source = source;
        storage = new StorageDriver<>(new HeapRecordStorage<>());
    }

    public void subscribe(RLPredicate<Record<K>> filter) {
        synchronized (this) {
            unsubscribe();
            subs = new Subscriber<>(null,filter,change -> {
                synchronized (this) {
                    storage.receive(change);
                }
            });
            source.subscribe(subs);
        }
    }

    public void unsubscribe() {
        synchronized (this) {
            if ( subs != null ) {
                source.unsubscribe(subs);
                subs = null;
            }
        }
    }

    public Record<K> getAnyRecord() {
        return null;
    }
}

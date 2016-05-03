package org.nustaq.reallive.client;

import org.nustaq.reallive.impl.StorageDriver;
import org.nustaq.reallive.impl.storage.HeapRecordStorage;
import org.nustaq.reallive.interfaces.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 22/08/15.
 *
 * Synchronized local replication of a subscriütion
 *
 */
public class SubscribedSet<K> {

    RealLiveTable<K> source;
    StorageDriver<K> storage;
    Subscriber<K> subs;

    public SubscribedSet(RealLiveTable<K> source) {
        this(source,false);
    }

    public SubscribedSet(RealLiveTable<K> source, boolean usConc) {
        this.source = source;
        storage = new StorageDriver<>(new HeapRecordStorage<>( usConc ? new ConcurrentHashMap<>() : new HashMap<>() ) );
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

    public Record<K> get(K key) {
        synchronized (this) {
            return storage.getStore().get(key);
        }
    }

    public Map<K,Record<K>> cloneMap() {
        HashMap res = new HashMap();
        synchronized (this) {
            storage.getStore().stream()
                .forEach( rec -> res.put(rec.getKey(),rec));
        }
        return res;
    }

    /**
     * should be used only if this is a HeapRecordStorage, accessed single threaded or this has been initialized using a concurrent map
     * @return
     */
    public Map<K,Record<K>> getMap() {
        return ((HeapRecordStorage)storage.getStore()).getMap();
    }

}

package org.nustaq.reallive.actors;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.FilterProcessor;
import org.nustaq.reallive.impl.StorageDriver;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by ruedi on 06.08.2015.
 */
public class RealLiveStream<K,V extends Record<K>> extends Actor<RealLiveStream<K,V>> implements ChangeReceiver<K,V>, Mutation<K,V>, RecordIterable<K,V>, ChangeStream<K,V> {

    StorageDriver<K,V> storageDriver;
    FilterProcessor<K,V> filterProcessor;

    @Local
    public void init(RecordStorage<K, V> store) {
        storageDriver = new StorageDriver<>(store);
        filterProcessor = new FilterProcessor<>(store);
        storageDriver.setListener(filterProcessor);
    }


    @Override
    public void receive(ChangeMessage<K, V> change) {
        storageDriver.receive(change);
    }

    @Override
    public void add(K key, Object... keyVals) {
        storageDriver.add(key,keyVals);
    }

    @Override
    public void update(K key, Object... keyVals) {
        storageDriver.update(key,keyVals);
    }

    @Override
    public void remove(K key) {
        storageDriver.remove(key);
    }

    @Override
    public void forEach(Predicate<V> filter, @InThread Consumer<V> action) {
        storageDriver.getStore().forEach(filter,action);
    }

    @Override
    @CallerSideMethod public void subscribe(Subscriber<K, V> subs) {
        // need callerside to inject inThread
        _subscribe(new Subscriber<>(subs.getFilter(), inThread(sender.get(), subs.getReceiver())));
    }

    public void _subscribe(Subscriber<K, V> subs) {
        filterProcessor.subscribe(subs);
    }

    @Override
    public void unsubscribe(Subscriber<K, V> subs) {

    }
}

package org.nustaq.reallive.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.StorageDriver;

import java.util.concurrent.*;
import java.util.function.*;

/**
 * Created by ruedi on 06.08.2015.
 *
 * FIXME: missing
 * - CAS
 * - SnapFin
 * - originator
 */
public class RealLiveStreamActor<K,V extends Record<K>> extends Actor<RealLiveStreamActor<K,V>> implements RealLiveTable<K,V> {

    StorageDriver<K,V> storageDriver;
    FilterProcessorActor<K,V> filterProcessor;
    ConcurrentHashMap<Subscriber,Subscriber> subsMap;

    @Local
    public void init( Supplier<RecordStorage<K, V>> storeFactory, boolean dedicatedFilterThread) {
        self().subsMap = new ConcurrentHashMap<>();
         RecordStorage<K, V> store = storeFactory.get();
        storageDriver = new StorageDriver<>(store);
        if (dedicatedFilterThread) {
            filterProcessor = Actors.AsActor(FilterProcessorActor.class);
        } else {
            filterProcessor = Actors.AsActor(FilterProcessorActor.class, getScheduler());
        }
        filterProcessor.init(self());
        storageDriver.setListener(filterProcessor);
    }


    @Override
    public void receive(ChangeMessage<K, V> change) {
        checkThread();
        storageDriver.receive(change);
    }

    @Override
    public void put(K key, Object... keyVals) {
        checkThread();
        storageDriver.put(key, keyVals);
    }

    @Override
    public void addOrUpdate(K key, Object... keyVals) {
        checkThread();
        storageDriver.addOrUpdate(key, keyVals);
    }

    @Override
    public void add(K key, Object... keyVals) {
        checkThread();
        storageDriver.add(key,keyVals);
    }

    @Override
    public void update(K key, Object... keyVals) {
        checkThread();
        storageDriver.update(key,keyVals);
    }

    @Override
    public void remove(K key) {
        checkThread();
        storageDriver.remove(key);
    }

    @Override
    public void forEach(Predicate<V> filter, @InThread Consumer<V> action) {
        checkThread();
        storageDriver.getStore().forEach(filter,action);
    }

    @Override
    protected void hasStopped() {
        filterProcessor.stop();
    }

    @Override
    @CallerSideMethod public void subscribe(Subscriber<K, V> subs) {
        // need callerside to inject inThread
        Subscriber<K, V> newSubs = new Subscriber<>(subs.getFilter(), inThread(sender.get(), subs.getReceiver()));
        subsMap.put(subs,newSubs);
        _subscribe(newSubs);
    }

    public void _subscribe(Subscriber<K, V> subs) {
        checkThread();
        filterProcessor.subscribe(subs);
    }
    public void _unsubscribe(Subscriber<K, V> subs) {
        checkThread();
        filterProcessor.unsubscribe(subs);
    }

    @CallerSideMethod @Override
    public void unsubscribe(Subscriber<K, V> subs) {
        Subscriber realSubs = subsMap.get(subs);
        _unsubscribe(realSubs);
        subsMap.remove(subs);
    }

    @Override
    public IPromise<V> get(K key) {
        return resolve(storageDriver.getStore().get(key));
    }

    @Override
    public IPromise<Long> size() {
        return resolve(storageDriver.getStore().size());
    }

}

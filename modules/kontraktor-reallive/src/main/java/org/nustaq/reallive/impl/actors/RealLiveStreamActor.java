package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.Mutator;
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
public class RealLiveStreamActor<K,V extends Record<K>> extends Actor<RealLiveStreamActor<K,V>> implements RealLiveTable<K,V>, Mutatable<K,V> {

    StorageDriver<K,V> storageDriver;
    FilterProcessorActor<K,V> filterProcessor;
    ConcurrentHashMap<Subscriber,Subscriber> subsMap;
    TableDescription description;

    @Local
    public void init( Supplier<RecordStorage<K, V>> storeFactory, Scheduler filterScheduler, TableDescription desc) {
        self().subsMap = new ConcurrentHashMap<>();
        this.description = desc;
        RecordStorage<K, V> store = storeFactory.get();
        storageDriver = new StorageDriver<>(store);
        filterProcessor = Actors.AsActor(FilterProcessorActor.class, filterScheduler);
        filterProcessor.init(self());
        storageDriver.setListener(filterProcessor);
    }


    @Override
    public void receive(ChangeMessage<K, V> change) {
        checkThread();
        storageDriver.receive(change);
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

    @Override @CallerSideMethod
    public Mutation<K, V> getMutation() {
        return new Mutator<>(self());
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return resolve(description);
    }
}

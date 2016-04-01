package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class TableSharding<K> implements RealLiveTable<K> {

    ShardFunc<K> func;
    RealLiveTable<K> shards[];
    final ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap<>();
    private TableDescription description;

    public TableSharding(ShardFunc<K> func, RealLiveTable<K>[] shards, TableDescription desc) {
        this.func = func;
        this.shards = shards;
        this.description = desc;
    }

    @Override
    public void receive(ChangeMessage<K> change) {
        if ( change.getType() != ChangeMessage.QUERYDONE )
            shards[func.apply(change.getKey())].receive(change);
    }

    @Override
    public void subscribe(Subscriber<K> subs) {
        AtomicInteger doneCount = new AtomicInteger(shards.length);
        ChangeReceiver<K> receiver = subs.getReceiver();
        ArrayList<Subscriber> subsList = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable<K> shard = shards[i];
            Subscriber<K> shardSubs = new Subscriber<>(subs.getPrePatchFilter(),subs.getFilter(), change -> {
                if (change.getType() == ChangeMessage.QUERYDONE) {
                    int count = doneCount.decrementAndGet();
                    if (count == 0) {
                        receiver.receive(change);
                    }
                } else {
                    receiver.receive(change);
                }
            });
            subsList.add(shardSubs);
            shard.subscribe(shardSubs);
        }
        subsMap.put(subs,subsList);
    }

    @Override
    public void unsubscribe(Subscriber<K> subs) {
        if ( subs == null ) {
            Log.Warn(this,"unsubscribed is null");
            return;
        }
        List<Subscriber> subscribers = subsMap.get(subs);
        if ( subscribers == null ) {
            Log.Warn(this,"unknown subscriber to unsubscribe "+subs);
            return;
        }
        for (int i = 0; i < subscribers.size(); i++) {
            Subscriber subscriber = subscribers.get(i);
            shards[i].unsubscribe(subscriber);
        }
        subsMap.remove(subs);
    }

    @Override
    public IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object... keyVals) {
        return shards[func.apply(key)].getMutation().putCAS(casCondition,key, keyVals);
    }

    @Override
    public void atomic(K key, RLConsumer<Record<K>> action) {
        shards[func.apply(key)].getMutation().atomic(key, action);
    }

    protected class ShardMutation implements Mutation<K> {

        @Override
        public IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object... keyVals) {
            return shards[func.apply(key)].getMutation().putCAS(casCondition, key, keyVals);
        }

        @Override
        public void put(K key, Object... keyVals) {
            shards[func.apply(key)].receive(new PutMessage<K>(RLUtil.get().record(key,keyVals)));
        }

        @Override
        public void atomic(K key, RLConsumer action) {
            shards[func.apply(key)].getMutation().atomic(key, action);
        }

        @Override
        public void addOrUpdate(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().addOrUpdate(key, keyVals));
        }

        @Override
        public void add(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().add(key, keyVals));
        }

        @Override
        public void add(Record<K> rec) {
            if ( rec instanceof RecordWrapper )
                rec = ((RecordWrapper) rec).getRecord();
            shards[func.apply(rec.getKey())].receive((ChangeMessage<K>)new AddMessage<>(rec));
        }

        @Override
        public void addOrUpdateRec(Record<K> rec) {
            if ( rec instanceof RecordWrapper )
                rec = ((RecordWrapper) rec).getRecord();
            shards[func.apply(rec.getKey())].receive(new AddMessage<K>(true,rec));
        }

        @Override
        public void put(Record<K> rec) {
            if ( rec instanceof RecordWrapper )
                rec = ((RecordWrapper) rec).getRecord();
            shards[func.apply(rec.getKey())].receive(new PutMessage<K>(rec));
        }

        @Override
        public void update(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().update(key, keyVals));
        }

        @Override
        public void remove(K key) {
            RemoveMessage remove = RLUtil.get().remove(key);
            shards[func.apply(key)].receive(remove);
        }
    }

    public Mutation<K> getMutation() {
        return new ShardMutation();
    }

    @Override
    public <T> void forEach(Spore<Record<K>, T> spore) {
        spore.setExpectedFinishCount(shards.length);
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable<K> shard = shards[i];
            shard.forEach(spore);
        }
    }

    @Override
    public IPromise ping() {
        List<IPromise<Object>> futs = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable<K> shard = shards[i];
            futs.add(shard.ping());
        }
        return Actors.all(futs);
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return new Promise(description);
    }

    public void stop() {
        for (int i = 0; i < shards.length; i++) {
            shards[i].stop();
        }
    }

    @Override
    public IPromise<StorageStats> getStats() {
        IPromise<StorageStats>[] shardStats = Actors.all(shards.length, i -> shards[i].getStats()).await();
        StorageStats stats = new StorageStats();
        for (int i = 0; i < shardStats.length; i++) {
            StorageStats storageStats = shardStats[i].get();
            storageStats.addTo(stats);
        }
        return new Promise<>(stats);
    }

    @Override
    public IPromise<Record<K>> get(K key) {
        if ( key == null )
            return null;
        return shards[func.apply(key)].get(key);
    }

    @Override
    public IPromise<Long> size() {
        Promise result = new Promise();
        List<IPromise<Long>> futs = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable<K> shard = shards[i];
            futs.add(shard.size());
        }
        Actors.all(futs).then( longPromisList -> {
            long sum = longPromisList.stream().mapToLong(prom -> prom.get()).sum();
            result.resolve(sum);
        });
        return result;
    }
}
